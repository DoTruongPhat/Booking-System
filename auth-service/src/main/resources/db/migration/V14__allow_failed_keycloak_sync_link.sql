BEGIN;

ALTER TABLE auth.user_kc_links
    ALTER COLUMN kc_user_id DROP NOT NULL;

DROP INDEX IF EXISTS auth.idx_kc_links_kc_user_id;
CREATE UNIQUE INDEX IF NOT EXISTS idx_kc_links_kc_user_id
    ON auth.user_kc_links(kc_user_id)
    WHERE kc_user_id IS NOT NULL;

COMMIT;
