
ALTER TABLE auth.tokens
    ALTER COLUMN token_encrypted DROP NOT NULL;
