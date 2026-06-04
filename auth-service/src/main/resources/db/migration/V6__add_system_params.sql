    CREATE TABLE IF NOT EXISTS auth.system_params (
                                                  key         VARCHAR(100) PRIMARY KEY,
    value       TEXT         NOT NULL,
    description VARCHAR(255),
    updated_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_by  VARCHAR(100)
    );

INSERT INTO auth.system_params (key, value, description) VALUES
                                                             ('MAX_LOGIN_ATTEMPTS',    '5',     'Max failed login attempts before lock'),
                                                             ('LOCK_DURATION_MINUTES', '15',    'Account lock duration in minutes'),
                                                             ('TOKEN_TTL_HOURS',       '720',   'JWT tokenEntity TTL in hours'),
                                                             ('MFA_SESSION_TTL_MINUTES','5',    'MFA session tokenEntity TTL in minutes'),
                                                             ('RESET_TOKEN_TTL_MINUTES','15',   'Password reset tokenEntity TTL in minutes');