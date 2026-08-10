DO $$
DECLARE
    item RECORD;
BEGIN
    FOR item IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'workflow'
          AND tablename LIKE 'act\_%' ESCAPE '\'
    LOOP
        EXECUTE format('ALTER TABLE workflow.%I SET SCHEMA public', item.tablename);
    END LOOP;
END $$;
