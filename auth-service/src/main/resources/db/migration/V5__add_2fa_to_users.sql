ALTER TABLE auth.users
    ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(500),
    ADD COLUMN IF NOT EXISTS two_factor_enabled_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS auth.two_factor_auth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    secret_encrypted VARCHAR(500) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    enabled_at TIMESTAMPTZ
);
