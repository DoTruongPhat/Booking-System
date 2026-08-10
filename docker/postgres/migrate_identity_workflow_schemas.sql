CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS workflow;

DO $$
DECLARE
    item RECORD;
BEGIN
    FOR item IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'auth'
    LOOP
        EXECUTE format('ALTER TABLE auth.%I SET SCHEMA identity', item.tablename);
    END LOOP;
END $$;
