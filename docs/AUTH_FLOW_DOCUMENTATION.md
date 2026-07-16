# AUTH FLOW DOCUMENTATION — SmartBooking System
## Tất cả flow chi tiết: tạo gì, lưu gì, ở đâu

---

## 1. REGISTER (Đăng ký Local)

### Flow
```
FE Form → POST /api/auth/register → BE validate → tạo user → Kafka → Email
```

### Chi tiết từng bước
```
1. FE gửi: { fullName, username, email, phone, password, timeZone }
2. BE validate:
   - username: 3-100 ký tự, chỉ a-z, 0-9, _
   - email: format hợp lệ + chưa tồn tại trong DB
   - password: ≥8 ký tự, có uppercase, lowercase, số, ký tự đặc biệt
3. BE hash password:
   - Tạo UUID → dùng làm salt
   - BCrypt(password + salt) → passwordHash
4. BE tạo User trong DB
5. BE gán role USER (default)
6. Kafka publish UserRegisteredEvent
7. Consumer gửi email chào mừng qua Gmail SMTP
8. Trả 201 Created
```

### Dữ liệu tạo/lưu

| Nơi lưu | Table/Key | Dữ liệu |
|----------|-----------|----------|
| PostgreSQL | `auth.users` | id, username, email, passwordHash, passwordSalt, timezone, authSource=LOCAL, emailVerified=false, isActive=true |
| PostgreSQL | `auth.user_roles` | user_id → role_id (USER) |
| Kafka | topic `email-notifications` | Event: {userId, username, email} |

---

## 2. LOGIN LOCAL (Đăng nhập bằng form)

### Flow
```
FE Form → POST /api/auth/login → verify password → check 2FA → generate JWT → set cookies
```

### Chi tiết từng bước
```
1. FE gửi: { username, password }
2. BE tìm user theo username → không thấy → AUTH_001
3. Check isLocked → nếu locked → AUTH_002
4. Check passwordHash == null → nếu null → AUTH_014 (SSO user)
5. BCrypt verify(password, passwordHash, salt):
   - Sai → incrementFailedAttempts
   - Sai ≥5 lần → lockUntil = now + 15 phút
   - Đúng → resetFailedAttempts
6. Load roles
7. Check twoFactorEnabled:
   - true → trả { twoFactorRequired: true, mfaSessionToken: xxx }
   - false → tiếp tục
8. Kill old sessions (single session):
   - UPDATE auth.tokens SET deactivation_reason='NEW_LOGIN' WHERE user_id=xxx
9. Generate JWT:
   - accessToken: HS256, TTL 1h, claims: {sub, username, roles, jti}
   - refreshToken: HS256, TTL 7 ngày, claims: {sub, jti}
10. Lưu refresh token vào DB
11. Set HttpOnly cookies:
    - access_token (max-age 1h)
    - refresh_token (max-age 7 ngày)
12. Trả LoginResponse
```

### Dữ liệu tạo/lưu

| Nơi lưu | Table/Key | Dữ liệu |
|----------|-----------|----------|
| PostgreSQL | `auth.tokens` | id, user_id, tokenHash(refreshToken), jti, active=true, ipAddress, userAgent, expiresAt |
| Cookie (HttpOnly) | `access_token` | JWT string, 1h TTL |
| Cookie (HttpOnly) | `refresh_token` | JWT string, 7 ngày TTL |
| FE localStorage | `user` | {username, email, roles, timezone} (không nhạy cảm) |

---

## 3. LOGIN KEYCLOAK (SSO — 3 Cases)

### Flow tổng quan
```
FE → redirect KC → user login → KC redirect callback → FE POST /exchange → BE xử lý 3 cases
```

### Chi tiết Phase 1: FE → KC
```
1. FE generate code_verifier (32 bytes random → base64url)
2. FE SHA256(code_verifier) → code_challenge
3. Lưu code_verifier vào sessionStorage
4. Redirect browser → KC authorization URL:
   - client_id=booking-frontend
   - response_type=code
   - redirect_uri=http://localhost:4200/auth/callback
   - code_challenge=xxx
   - code_challenge_method=S256
   - scope=openid email profile
   - prompt=login (luôn hiện form)
5. User nhập username/password trên KC form
6. KC redirect → http://localhost:4200/auth/callback?code=xxx
```

### Chi tiết Phase 2: FE callback → BE exchange
```
1. FE lấy code từ URL query param
2. FE lấy code_verifier từ sessionStorage
3. FE POST /api/auth/exchange { code, codeVerifier, redirectUri, timeZone }
```

### Chi tiết Phase 3: BE exchange
```
1. BE gọi KC /token endpoint:
   - grant_type=authorization_code
   - code=xxx
   - code_verifier=xxx
   - client_id=booking-frontend
   - redirect_uri=xxx
   → KC trả: access_token, id_token, refresh_token

2. BE verify id_token:
   - Parse JWT header → lấy kid
   - Fetch JWKS từ KC (cache 1h) → lấy public key theo kid
   - Verify signature RS256
   - Check iss = http://localhost:8180/realms/booking
   - Check aud contains booking-frontend
   - Check exp > now
   
3. Extract claims:
   - sub (KC user ID)
   - email
   - email_verified
   - identity_provider (google/facebook/null)
   - realm_access.roles

4. Validate:
   - email == null → AUTH_013
   - email_verified == false → AUTH_010

5. Dispatch 3 cases (xem bên dưới)

6. Generate JWT nội bộ (giống login local)
7. Lưu refresh token vào auth.tokens
8. Lưu KC tokens vào auth.kc_tokens (cho SSO logout)
9. Set HttpOnly cookies
```

### Case A: Existing KC User (đã link trước)
```
Điều kiện: findByKcUserId(sub) → FOUND

Action:
- Update kcSyncedAt = now
- Update syncStatus = SYNCED
- syncVersion++
- Login (generate JWT)

DB changes: UPDATE auth.users SET kc_synced_at, sync_status, sync_version
```

### Case B: Account Linking (local user cùng email)
```
Điều kiện: 
  findByKcUserId(sub) → NOT FOUND
  findByEmailIgnoreCase(email) → FOUND

Checks:
  1. user.kcUserId != null && != sub → AUTH_012 (hijack protection)
  2. user.emailVerified == false → AUTH_011 (cần verify email trước)

Action:
- Update kcUserId = sub
- Update kcProvider = google/facebook/null
- Update authSource = LINKED (was LOCAL)
- Update kcSyncedAt, syncStatus, syncVersion
- Login (generate JWT)

DB changes: UPDATE auth.users SET kc_user_id, kc_provider, auth_source, kc_synced_at, sync_status, sync_version
```

### Case C: Auto-Create (user mới)
```
Điều kiện:
  findByKcUserId(sub) → NOT FOUND
  findByEmailIgnoreCase(email) → NOT FOUND

Action:
- Resolve username: email prefix → john → john1 → john2 (nếu trùng)
- Tạo user mới:
  - passwordHash = null (passwordless)
  - passwordSalt = null
  - emailVerified = true (KC đã verify)
  - authSource = KEYCLOAK
  - kcUserId = sub
  - kcProvider = identity_provider claim
  - role = USER (default)
  - timezone = FE gửi hoặc UTC
- Login (generate JWT)

DB creates:
  INSERT auth.users (...)
  INSERT auth.user_roles (user_id, role_id)
```

### Dữ liệu tạo/lưu (tất cả cases)

| Nơi lưu | Table/Key | Dữ liệu |
|----------|-----------|----------|
| PostgreSQL | `auth.users` | Tùy case: update hoặc insert |
| PostgreSQL | `auth.tokens` | refreshToken hash, jti, expiry |
| PostgreSQL | `auth.kc_tokens` | KC access_token, refresh_token (cho SSO logout) |
| Cookie (HttpOnly) | `access_token` | JWT 1h |
| Cookie (HttpOnly) | `refresh_token` | JWT 7 ngày |
| FE localStorage | `user` | {username, email, roles, timezone} |
| FE sessionStorage | `kc_code_verifier` | PKCE verifier (xóa sau exchange) |

---

## 4. FORGOT PASSWORD (Quên mật khẩu)

### Flow
```
FE → POST /forgot-password → BE generate OTP → Redis + DB → Gmail SMTP → User
```

### Chi tiết từng bước
```
1. FE gửi: { email }
2. BE tìm user theo email → không thấy → vẫn trả 200 (chống email enumeration)
3. BE generate OTP:
   - Random 6 số
   - BCrypt hash OTP → otpHash
4. Lưu OTP vào Redis:
   - Key: OTP:{email}:FORGOT_PASSWORD
   - Value: otpHash
   - TTL: 10 phút
5. Lưu OTP vào DB (audit trail):
   - Table: auth.otp_verifications
6. Gửi email qua Gmail SMTP:
   - Subject: "Reset your SmartBooking password"
   - Body: HTML với OTP 6 số
7. Trả 200 OK (dù email tồn tại hay không)
```

### Dữ liệu tạo/lưu

| Nơi lưu | Table/Key | Dữ liệu | TTL |
|----------|-----------|----------|-----|
| Redis | `OTP:{email}:FORGOT_PASSWORD` | BCrypt hash OTP | 10 phút |
| PostgreSQL | `auth.otp_verifications` | email, purpose, otpHash, attempts, maxAttempts, expiresAt | — |
| Gmail | Inbox người nhận | Email HTML chứa OTP 6 số | — |

---

## 5. RESET PASSWORD (Đặt lại mật khẩu)

### Flow
```
FE → POST /reset-password → BE verify OTP → update password → xóa OTP
```

### Chi tiết từng bước
```
1. FE gửi: { email, otp, newPassword }
2. BE validate newPassword (≥8, uppercase, lowercase, số, đặc biệt)
3. BE lấy OTP hash từ Redis:
   - Key: OTP:{email}:FORGOT_PASSWORD
   - Không tìm thấy → "OTP hết hạn"
4. BCrypt verify(otp, otpHash):
   - Sai → incrementAttempts
   - Quá 5 lần → xóa OTP, yêu cầu gửi lại
   - Đúng → tiếp tục
5. Tìm user theo email
6. Hash password mới:
   - BCrypt(newPassword + salt) → passwordHash mới
7. Update DB:
   - UPDATE auth.users SET password_hash, password_salt, updated_at
8. Xóa OTP khỏi Redis
9. Update OTP DB: SET used_at = now
10. Kill tất cả sessions cũ (force re-login)
11. Trả 200 OK
```

### Dữ liệu thay đổi

| Nơi lưu | Action | Dữ liệu |
|----------|--------|----------|
| PostgreSQL | UPDATE `auth.users` | password_hash, password_salt |
| Redis | DELETE | `OTP:{email}:FORGOT_PASSWORD` |
| PostgreSQL | UPDATE `auth.otp_verifications` | used_at = now |
| PostgreSQL | UPDATE `auth.tokens` | deactivation_reason = PASSWORD_RESET |

---

## 6. REFRESH TOKEN (Tự động gia hạn)

### Flow
```
FE request → 401 → interceptor → POST /refresh → BE rotate → new cookies → retry request
```

### Chi tiết từng bước
```
1. FE gọi API bất kỳ → BE trả 401 (access_token hết hạn)
2. FE interceptor tự động gọi POST /api/auth/refresh
3. BE đọc refresh_token từ HttpOnly cookie
4. BE verify refresh_token:
   - Decode JWT → lấy jti
   - Tìm trong auth.tokens theo jti + active=true
   - Check expiresAt > now
   - Check tokenHash match
5. Generate JWT mới:
   - New jti
   - New accessToken (1h)
   - New refreshToken (7 ngày)
6. Deactivate old token:
   - UPDATE auth.tokens SET active=false, deactivation_reason=ROTATED
7. Save new token:
   - INSERT auth.tokens (new jti, new hash)
8. Set new cookies
9. FE interceptor retry request gốc với cookie mới
```

### Dữ liệu thay đổi

| Nơi lưu | Action | Dữ liệu |
|----------|--------|----------|
| PostgreSQL | UPDATE `auth.tokens` (old) | active=false, deactivation_reason=ROTATED |
| PostgreSQL | INSERT `auth.tokens` (new) | new jti, tokenHash, expiresAt |
| Cookie | REPLACE | access_token mới, refresh_token mới |

---

## 7. LOGOUT (Đăng xuất)

### Flow
```
FE → POST /logout → BE invalidate token → xóa cookies
```

### Chi tiết từng bước
```
1. FE gọi POST /api/auth/logout
2. BE đọc access_token từ cookie hoặc Authorization header
3. BE decode JWT → lấy jti
4. Deactivate token trong DB:
   - UPDATE auth.tokens SET active=false, deactivation_reason=LOGOUT
5. Xóa cookies:
   - Set-Cookie: access_token=; max-age=0
   - Set-Cookie: refresh_token=; max-age=0
6. FE xóa localStorage (user info)
7. FE redirect → /auth/login
```

### Dữ liệu thay đổi

| Nơi lưu | Action | Dữ liệu |
|----------|--------|----------|
| PostgreSQL | UPDATE `auth.tokens` | active=false, deactivation_reason=LOGOUT |
| Cookie | DELETE | access_token, refresh_token |
| FE localStorage | DELETE | `user` key |

---

## 8. 2FA SETUP (Bật xác thực 2 bước)

### Flow
```
User login → POST /2fa/setup → QR code → scan Authenticator → POST /2fa/enable → verify
```

### Chi tiết từng bước
```
1. User đã login (có access_token cookie)
2. POST /api/auth/2fa/setup
3. BE generate TOTP secret:
   - Random 20 bytes → Base32 encode
   - Build otpauth:// URI
   - Generate QR code image (base64)
4. Trả { secret, qrCodeUrl, otpAuthUri }
5. User scan QR bằng Google Authenticator / Authy
6. User nhập code 6 số từ app
7. POST /api/auth/2fa/enable { otp: "123456" }
8. BE verify TOTP:
   - TOTP.verify(otp, secret, window=1)
   - Đúng → UPDATE users SET totp_secret=secret, two_factor_enabled=true
   - Sai → 400 error
```

### Dữ liệu tạo/lưu

| Nơi lưu | Table/Key | Dữ liệu |
|----------|-----------|----------|
| PostgreSQL | `auth.users` | totp_secret (encrypted), two_factor_enabled=true |
| User's phone | Authenticator App | TOTP secret (qua QR scan) |

---

## 9. 2FA VERIFY (Xác thực khi login)

### Flow
```
Login → twoFactorRequired=true → FE hiện form OTP → POST /2fa/verify → JWT
```

### Chi tiết từng bước
```
1. Login thành công (password đúng)
2. BE phát hiện twoFactorEnabled=true
3. BE tạo mfaSessionToken:
   - Random UUID
   - Lưu vào Redis: MFA:{token} → userId (TTL 5 phút)
4. Trả { twoFactorRequired: true, mfaSessionToken: xxx }
   (KHÔNG trả JWT, KHÔNG set cookie)
5. FE hiện form nhập 6 số
6. User mở Authenticator → lấy code
7. POST /api/auth/2fa/verify { mfaSessionToken, otp }
8. BE verify:
   - Lấy userId từ Redis theo mfaSessionToken
   - Load user → lấy totpSecret
   - TOTP.verify(otp, secret)
   - Đúng → generate JWT, set cookies (giống login thường)
   - Sai → 401
9. Xóa mfaSessionToken khỏi Redis
```

### Dữ liệu tạo/lưu

| Nơi lưu | Table/Key | Dữ liệu | TTL |
|----------|-----------|----------|-----|
| Redis | `MFA:{mfaSessionToken}` | userId | 5 phút |
| PostgreSQL | `auth.tokens` | refreshToken (sau verify thành công) | — |
| Cookie | `access_token`, `refresh_token` | JWT (sau verify thành công) | 1h / 7d |

---

## 10. SINGLE SESSION (Chỉ 1 phiên hoạt động)

### Flow
```
User login thiết bị 2 → kill session thiết bị 1 → thiết bị 1 bị 401
```

### Chi tiết
```
1. User login trên Browser A → session A active
2. User login trên Browser B:
   - BE: deactivateAllByUserId(userId, "NEW_LOGIN")
   - UPDATE auth.tokens SET active=false WHERE user_id=xxx AND active=true
   - → Session A bị kill
   - Tạo session B mới
3. Browser A gọi API:
   - BE verify token → tìm trong auth.tokens → active=false
   - → 401 Unauthorized
4. FE Browser A hiện modal "Phiên hết hạn"
```

---

## 11. ADMIN: ASSIGN ROLE

### Flow
```
Admin → POST /admin/users/{id}/roles → replace roles → save
```

### Chi tiết
```
1. Admin chọn user + role mới
2. POST /api/admin/users/{id}/roles { roleCode: "HOST" }
3. BE tìm user → tìm role
4. Replace toàn bộ roles: user.setRoles(Set.of(newRole))
5. Save → DB update auth.user_roles
```

### Dữ liệu thay đổi

| Nơi lưu | Action |
|----------|--------|
| PostgreSQL `auth.user_roles` | DELETE old rows + INSERT new row |

---

## 12. ADMIN: RESET PASSWORD

### Flow
```
Admin → PUT /admin/users/{id}/password → hash new password → save
```

### Chi tiết
```
1. Admin nhập password mới cho user
2. PUT /api/admin/users/{id}/password { newPassword }
3. BE hash: BCrypt(newPassword + salt)
4. UPDATE auth.users SET password_hash
5. Không kill session (admin có thể chọn revoke riêng)
```

---

## TỔNG KẾT DATABASE SCHEMA

### auth.users
```
id, username, email, password_hash, password_salt,
is_active, is_locked, failed_attempts, locked_until,
timezone, totp_secret, two_factor_enabled,
phone, kc_user_id, kc_synced_at, kc_provider,
email_verified, sync_status, sync_version, auth_source,
created_at, updated_at
```

### auth.tokens (refresh token tracking)
```
id, user_id, token_hash, jti, active,
ip_address, user_agent, created_at, expires_at,
deactivation_reason (NEW_LOGIN | LOGOUT | ROTATED | PASSWORD_RESET)
```

### auth.kc_tokens (KC tokens for SSO logout)
```
user_id, kc_user_id, kc_access_token, kc_refresh_token,
access_token_expires_at, refresh_token_expires_at, last_refreshed_at
```

### auth.otp_verifications (OTP audit trail)
```
id, user_id, email, purpose, otp_hash,
attempts, max_attempts, expires_at, used_at, created_at
```

### auth.user_roles (many-to-many)
```
user_id, role_id
```

### Redis keys
```
OTP:{email}:{purpose}     → BCrypt hash OTP (TTL 10 min)
MFA:{mfaSessionToken}     → userId (TTL 5 min)
token:blacklist:{jti}     → "1" (TTL = token remaining TTL)
```

### Cookies (HttpOnly, Lax, not Secure in dev)
```
access_token   → JWT (1h)
refresh_token  → JWT (7 days)
```

### FE localStorage
```
user → { username, email, roles, timezone }
```

### FE sessionStorage
```
kc_code_verifier  → PKCE verifier (xóa sau exchange)
kc_exchanging     → code đang exchange (chống double call)
```
