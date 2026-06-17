-- =============================================
-- V8: Phone, KC sync fields, user_sessions, kc_tokens
-- Chuẩn bị cho Keycloak SSO + sync 2 chiều
-- =============================================

-- ── 1. ALTER users: thêm phone + KC sync fields ──────
ALTER TABLE auth.users
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN kc_user_id VARCHAR(100),
    ADD COLUMN kc_synced_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN sync_status VARCHAR(20) DEFAULT 'PENDING',
    ADD COLUMN sync_version BIGINT DEFAULT 0,
    ADD COLUMN auth_source VARCHAR(20) DEFAULT 'LOCAL';
    -- LOCAL: chỉ Form A local
    -- KEYCLOAK: chỉ Form B (KC)
    -- LINKED: có cả 2

-- sync_status:
-- PENDING: chưa sync sang KC
-- SYNCED: đã sync
-- FAILED: sync lỗi, cần retry
-- DELETED: user đã xóa ở KC

-- Index cho phone (unique partial - chỉ enforce khi phone not null)
CREATE UNIQUE INDEX idx_users_phone_unique
    ON auth.users(phone) WHERE phone IS NOT NULL;

-- Index cho kc_user_id (đã có thể unique, nhưng thêm để chắc)
CREATE UNIQUE INDEX idx_users_kc_user_id_unique
    ON auth.users(kc_user_id) WHERE kc_user_id IS NOT NULL;

-- Index cho sync_status (query user cần sync)
CREATE INDEX idx_users_sync_status
    ON auth.users(sync_status) WHERE sync_status = 'PENDING';

-- Index cho email (sync lookup)
CREATE INDEX idx_users_email_lower
    ON auth.users(LOWER(email));

-- ── 2. CREATE user_sessions: Single session tracking ──────
CREATE TABLE auth.user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    session_id VARCHAR(100) UNIQUE NOT NULL,
    jti VARCHAR(100) UNIQUE NOT NULL,           -- JWT ID
    auth_source VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    -- LOCAL: Form A login
    -- KEYCLOAK: Form B (KC) login
    device_info VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    invalidated_at TIMESTAMP WITH TIME ZONE,
    invalidation_reason VARCHAR(50)
    -- NEW_LOGIN, LOGOUT, ADMIN_KILL, TOKEN_EXPIRED
);

-- Index cho query session active theo user
CREATE INDEX idx_user_sessions_user_active
    ON auth.user_sessions(user_id)
    WHERE invalidated_at IS NULL;

-- Index cho query theo jti (verify mỗi request)
CREATE UNIQUE INDEX idx_user_sessions_jti
    ON auth.user_sessions(jti);

-- Index cho cleanup expired sessions
CREATE INDEX idx_user_sessions_expires_at
    ON auth.user_sessions(expires_at)
    WHERE invalidated_at IS NULL;

-- ── 3. CREATE kc_tokens: Keycloak refresh_token storage ──────
CREATE TABLE auth.kc_tokens (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    kc_user_id VARCHAR(100) NOT NULL,
    kc_access_token TEXT NOT NULL,             -- encrypted at rest
    kc_refresh_token TEXT NOT NULL,           -- encrypted at rest
    access_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_refreshed_at TIMESTAMP WITH TIME ZONE
);

-- Index cho lookup theo kc_user_id (khi cần)
CREATE INDEX idx_kc_tokens_kc_user_id
    ON auth.kc_tokens(kc_user_id);

-- ── 4. CREATE otp_verifications: Backup cho Redis OTP ──────
-- Redis là primary, DB này là audit trail
CREATE TABLE auth.otp_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    -- FORGOT_PASSWORD, CHANGE_EMAIL, DELETE_ACCOUNT, PHONE_VERIFY
    otp_hash VARCHAR(255) NOT NULL,           -- BCrypt hash
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index cho query theo email + purpose
CREATE INDEX idx_otp_email_purpose
    ON auth.otp_verifications(email, purpose)
    WHERE used_at IS NULL;

-- Index cho cleanup expired OTP
CREATE INDEX idx_otp_expires_at
    ON auth.otp_verifications(expires_at)
    WHERE used_at IS NULL;

-- ── 5. CREATE audit_log: Track session/auth events ──────
CREATE TABLE auth.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    -- LOGIN_LOCAL, LOGIN_KC, LOGOUT, SESSION_KILLED, OTP_SENT, OTP_VERIFIED, PHONE_UPDATE, ...
    source VARCHAR(20) NOT NULL,
    -- LOCAL, KEYCLOAK, ADMIN, SYSTEM
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index cho query theo user
CREATE INDEX idx_audit_log_user_created
    ON auth.audit_log(user_id, created_at DESC);

-- Index cho query theo action (analytics)
CREATE INDEX idx_audit_log_action
    ON auth.audit_log(action, created_at DESC);

-- ── 6. Comments cho documentation ──────
COMMENT ON TABLE auth.user_sessions IS
    'Single session tracking - mỗi user chỉ có 1 session active';
COMMENT ON TABLE auth.kc_tokens IS
    'Keycloak tokens storage - encrypted at rest';
COMMENT ON TABLE auth.otp_verifications IS
    'OTP audit trail - Redis là primary, DB là backup';
COMMENT ON TABLE auth.audit_log IS
    'Auth events audit log';
