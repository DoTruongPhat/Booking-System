-- V10__redesign_tokens_and_blacklist.sql
-- ============================================================
-- MỤC ĐÍCH:
-- 1. Bỏ cột token_encrypted (security)
-- 2. Bỏ cột last_used_at (không cần inactive timeout)
-- 3. Set jti NOT NULL + thêm expires_at cho tokens
-- 4. Tạo bảng tokens_blacklist (lưu jti bị revoke)
-- 5. Bỏ bảng user_sessions
--
-- DESIGN OVERVIEW:
-- - Access token  (1h) : stateless, KHÔNG lưu DB
--                        Verify: check blacklist by jti (Redis)
-- - Refresh token (7d) : hash SHA-256 lưu vào auth.tokens
--                        Verify: hash query + check blacklist + is_active
-- - Blacklist          : jti bị revoke (cả access lẫn refresh)
--
-- REVOKE POLICY (Cách A — chấp nhận delay 1h):
-- - Admin revoke user → kill refresh ngay, access tự hết khi exp
-- - User logout       → blacklist cả access_jti và refresh_jti
-- ============================================================


-- ============================================================
-- PHASE 1: Backfill jti cho row cũ
-- → Bắt buộc trước khi SET NOT NULL ở Phase 3
-- ============================================================
UPDATE auth.tokens
SET jti = gen_random_uuid()::text
WHERE jti IS NULL;


-- ============================================================
-- PHASE 2: Bỏ cột token_encrypted (security)
-- → Refresh token chỉ lưu HASH, không lưu raw
-- ============================================================
ALTER TABLE auth.tokens DROP COLUMN IF EXISTS token_encrypted;


-- ============================================================
-- PHASE 3: jti NOT NULL
-- → V3 đã có UNIQUE INDEX idx_tokens_jti
-- → Chỉ thiếu NOT NULL constraint
-- ============================================================
ALTER TABLE auth.tokens ALTER COLUMN jti SET NOT NULL;


-- ============================================================
-- PHASE 4: Thêm expires_at vào auth.tokens
-- → V1 thiếu cột này
-- → Refresh token cần TTL để biết khi nào hết hạn
-- → Backfill cho row cũ: created_at + 7 ngày
-- ============================================================
ALTER TABLE auth.tokens
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

UPDATE auth.tokens
SET expires_at = created_at + INTERVAL '7 days'
WHERE expires_at IS NULL;

ALTER TABLE auth.tokens ALTER COLUMN expires_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tokens_expires_active
    ON auth.tokens(expires_at) WHERE is_active = true;


-- ============================================================
-- PHASE 5: Bỏ cột last_used_at
-- → Không có feature inactive timeout
-- → Đã có expires_at để check TTL
-- ============================================================
ALTER TABLE auth.tokens DROP COLUMN IF EXISTS last_used_at;


-- ============================================================
-- PHASE 6: Tạo bảng tokens_blacklist
-- → Lưu jti bị revoke (logout, NEW_LOGIN, ADMIN_REVOKE)
-- → Áp dụng cho CẢ access token VÀ refresh token
-- → Check trong TokenAuthFilter mỗi request (Redis là chính)
--
-- Query patterns:
--   1. Verify token : SELECT 1 WHERE jti = ?        (PK lookup)
--   2. Check user   : SELECT * WHERE user_id = ?    (idx_blacklist_user)
--   3. Cleanup job  : DELETE WHERE expires_at < NOW() (idx_blacklist_expires)
-- ============================================================
CREATE TABLE IF NOT EXISTS auth.tokens_blacklist (
                                                     jti             VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id         UUID         NOT NULL,
    blacklisted_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ  NOT NULL,
    reason          VARCHAR(50)  NOT NULL,
    CONSTRAINT fk_blacklist_user
    FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_blacklist_user
    ON auth.tokens_blacklist(user_id);

CREATE INDEX IF NOT EXISTS idx_blacklist_expires
    ON auth.tokens_blacklist(expires_at);


-- ============================================================
-- PHASE 7: Bỏ bảng user_sessions
-- → auth.tokens đã track refresh token đủ thông tin
-- → CASCADE để xóa luôn FK nếu có (V8 không có ai reference)
-- ============================================================
DROP TABLE IF EXISTS auth.user_sessions CASCADE;