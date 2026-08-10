ALTER TABLE auth.audit_logs
    ADD COLUMN IF NOT EXISTS event_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS actor_id UUID,
    ADD COLUMN IF NOT EXISTS actor_external_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS actor_name VARCHAR(160),
    ADD COLUMN IF NOT EXISTS actor_role VARCHAR(60),
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS entity_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS trace_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

UPDATE auth.audit_logs
SET event_type = COALESCE(event_type, action),
    entity_type = COALESCE(entity_type, resource),
    entity_id = COALESCE(entity_id, resource_id::text)
WHERE event_type IS NULL
   OR entity_type IS NULL
   OR entity_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_logs_event_key
    ON auth.audit_logs(event_key)
    WHERE event_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
    ON auth.audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type
    ON auth.audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_source
    ON auth.audit_logs(source);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_name
    ON auth.audit_logs(actor_name);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON auth.audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_trace_id
    ON auth.audit_logs(trace_id);
