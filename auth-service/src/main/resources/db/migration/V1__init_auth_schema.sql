CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

CREATE TABLE IF NOT EXISTS auth.users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(500) NOT NULL,
    password_salt   VARCHAR(255) NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    timezone        VARCHAR(50)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
    );

CREATE TABLE IF NOT EXISTS auth.roles (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_roles_code UNIQUE (code)
    );

CREATE TABLE IF NOT EXISTS auth.permissions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(150) NOT NULL,
    name        VARCHAR(200) NOT NULL,
    resource    VARCHAR(100) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_permissions_code    UNIQUE (code),
    CONSTRAINT uq_permissions_res_act UNIQUE (resource, action)
    );

CREATE TABLE IF NOT EXISTS auth.role_permissions (
    role_id       UUID NOT NULL REFERENCES auth.roles(id)       ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES auth.permissions(id) ON DELETE CASCADE,
    granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id)
    );

CREATE TABLE IF NOT EXISTS auth.user_roles (
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES auth.roles(id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
    );

CREATE TABLE IF NOT EXISTS auth.tokens (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash           VARCHAR(512) NOT NULL,
    token_encrypted      TEXT         NOT NULL,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    ip_address           VARCHAR(45),
    user_agent           VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deactivated_at       TIMESTAMPTZ,
    deactivation_reason  VARCHAR(100),
    CONSTRAINT uq_tokens_hash UNIQUE (token_hash)
    );

CREATE TABLE IF NOT EXISTS auth.two_factor_auth (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    secret_encrypted VARCHAR(500) NOT NULL,
    is_enabled       BOOLEAN      NOT NULL DEFAULT FALSE,
    backup_codes     JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    enabled_at       TIMESTAMPTZ
    );

CREATE TABLE IF NOT EXISTS auth.password_reset_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    is_used     BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reset_token UNIQUE (token_hash)
    );

CREATE TABLE IF NOT EXISTS auth.audit_logs (id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         REFERENCES auth.users(id),
    action      VARCHAR(100) NOT NULL,
    resource    VARCHAR(100),
    resource_id UUID,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
    );

CREATE INDEX idx_users_username     ON auth.users(username);
CREATE INDEX idx_users_email        ON auth.users(email);
CREATE INDEX idx_tokens_user_active ON auth.tokens(user_id, is_active);
CREATE INDEX idx_tokens_hash_active ON auth.tokens(token_hash, is_active);
CREATE INDEX idx_audit_user         ON auth.audit_logs(user_id);