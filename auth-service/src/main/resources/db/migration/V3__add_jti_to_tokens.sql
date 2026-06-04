ALTER TABLE auth.tokens
    ADD COLUMN IF NOT EXISTS jti VARCHAR(255);

-- Index để tìm nhanh theo jti
CREATE UNIQUE INDEX IF NOT EXISTS idx_tokens_jti
    ON auth.tokens(jti);