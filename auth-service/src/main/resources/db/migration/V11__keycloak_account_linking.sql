-- V11__keycloak_account_linking.sql
-- Phase C: Keycloak Account Linking
-- V8 đã có: kc_user_id, auth_source, idx_users_kc_user_id_unique
-- V11 bổ sung: email_verified, kc_provider, unique email

BEGIN;

ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS kc_provider VARCHAR(50);

-- Backfill LOCAL users
UPDATE auth.users
SET email_verified = TRUE
WHERE auth_source = 'LOCAL' AND email_verified = FALSE;

-- CHECK constraint provider
ALTER TABLE auth.users
    ADD CONSTRAINT chk_users_kc_provider
        CHECK (kc_provider IN ('google', 'facebook', 'github') OR kc_provider IS NULL);

-- Nâng email index → UNIQUE
DROP INDEX IF EXISTS auth.idx_users_email_lower;
CREATE UNIQUE INDEX idx_users_email_unique
    ON auth.users (LOWER(TRIM(email)));

-- Passwordless: cả 2 cột nullable
ALTER TABLE auth.users
    ALTER COLUMN password_hash DROP NOT NULL,
ALTER COLUMN password_salt DROP NOT NULL;   -- ← thêm dòng này

COMMENT ON COLUMN auth.users.email_verified IS 'TRUE nếu email đã verify (OTP hoặc KC)';
COMMENT ON COLUMN auth.users.kc_provider    IS 'google | facebook | github | NULL=local';

COMMIT;