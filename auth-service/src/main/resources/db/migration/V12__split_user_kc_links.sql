-- =============================================
-- V12__split_user_kc_links.sql
-- Tách KC sync columns từ auth.users → auth.user_kc_links
-- + Bỏ cột thừa khỏi auth.users
-- =============================================

BEGIN;

-- ══════════════════════════════════════════════
-- 1. Tạo bảng user_kc_links
-- ══════════════════════════════════════════════
CREATE TABLE auth.user_kc_links (
                                    user_id         UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
                                    kc_user_id      VARCHAR(100) NOT NULL,
                                    kc_provider     VARCHAR(50),         -- google, facebook, github, null (KC direct)
                                    auth_source     VARCHAR(20) NOT NULL DEFAULT 'KEYCLOAK',
    -- KEYCLOAK: chỉ KC login
    -- LINKED: có cả local password + KC
                                    kc_synced_at    TIMESTAMPTZ,
                                    sync_status     VARCHAR(20) DEFAULT 'SYNCED',
                                    sync_version    BIGINT DEFAULT 1,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_kc_links_kc_user_id
    ON auth.user_kc_links(kc_user_id);

COMMENT ON TABLE auth.user_kc_links IS 'Keycloak account linking - 1:1 với users, tách riêng KC sync logic';

-- ══════════════════════════════════════════════
-- 2. Migrate data từ users → user_kc_links
-- ══════════════════════════════════════════════
INSERT INTO auth.user_kc_links (user_id, kc_user_id, kc_provider, auth_source, kc_synced_at, sync_status, sync_version)
SELECT id, kc_user_id, kc_provider, auth_source, kc_synced_at, sync_status, sync_version
FROM auth.users
WHERE kc_user_id IS NOT NULL;

-- ══════════════════════════════════════════════
-- 3. Xóa cột KC khỏi auth.users
-- ══════════════════════════════════════════════
-- Drop indexes trước
DROP INDEX IF EXISTS auth.idx_users_kc_user_id_unique;
DROP INDEX IF EXISTS auth.idx_users_sync_status;

ALTER TABLE auth.users
DROP COLUMN IF EXISTS kc_user_id,
    DROP COLUMN IF EXISTS kc_provider,
    DROP COLUMN IF EXISTS kc_synced_at,
    DROP COLUMN IF EXISTS sync_status,
    DROP COLUMN IF EXISTS sync_version,
    DROP COLUMN IF EXISTS auth_source;

-- ══════════════════════════════════════════════
-- 4. Xóa bảng kc_tokens (không cần — session độc lập, KC token chỉ dùng 1 lần trong exchange)
-- ══════════════════════════════════════════════
DROP TABLE IF EXISTS auth.kc_tokens;

-- ══════════════════════════════════════════════
-- 5. auth.users giờ gọn lại:
-- id, username, email, password_hash, password_salt,
-- is_active, is_locked, failed_attempts, locked_until,
-- email_verified, timezone, phone,
-- totp_secret, two_factor_enabled,
-- created_at, updated_at
-- → 16 cột
-- ══════════════════════════════════════════════

COMMIT;