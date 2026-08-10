CREATE TABLE IF NOT EXISTS booking.hotel_change_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id            UUID        NOT NULL REFERENCES booking.hotels(id) ON DELETE CASCADE,
    owner_user_id       UUID        NOT NULL,
    proposed_changes    JSONB       NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    reviewer_id         VARCHAR(100),
    decision_comment    TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,

    CONSTRAINT chk_hotel_change_request_status CHECK (status IN (
        'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_hotel_change_requests_hotel
    ON booking.hotel_change_requests(hotel_id);
CREATE INDEX IF NOT EXISTS idx_hotel_change_requests_owner
    ON booking.hotel_change_requests(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_hotel_change_requests_status
    ON booking.hotel_change_requests(status);

CREATE TABLE IF NOT EXISTS booking.hotel_workflow_approvals (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hotel_id              UUID        NOT NULL REFERENCES booking.hotels(id) ON DELETE CASCADE,
    change_request_id     UUID        REFERENCES booking.hotel_change_requests(id) ON DELETE CASCADE,
    workflow_type         VARCHAR(30) NOT NULL,
    process_instance_id   VARCHAR(100),
    business_key          VARCHAR(100) NOT NULL,
    current_task_id       VARCHAR(100),
    current_task_name     VARCHAR(255),
    workflow_status       VARCHAR(40) NOT NULL DEFAULT 'START_REQUESTED',
    hotel_status_snapshot VARCHAR(30) NOT NULL,
    assignee              VARCHAR(100),
    decision              VARCHAR(30),
    rejection_reason      TEXT,
    started_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    last_synced_at        TIMESTAMPTZ,
    last_error            TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_hotel_workflow_business_key UNIQUE (business_key),
    CONSTRAINT chk_hotel_workflow_type CHECK (workflow_type IN ('CREATE_HOTEL', 'UPDATE_HOTEL')),
    CONSTRAINT chk_hotel_workflow_status CHECK (workflow_status IN (
        'START_REQUESTED', 'PROCESS_STARTED', 'WAITING_ADMIN_REVIEW', 'CLAIMED',
        'APPROVING', 'REJECTING', 'APPROVED', 'REJECTED', 'INCIDENT', 'OUT_OF_SYNC'
    )),
    CONSTRAINT chk_hotel_workflow_decision CHECK (
        decision IS NULL OR decision IN ('APPROVED', 'REJECTED')
    )
);

CREATE INDEX IF NOT EXISTS idx_hotel_workflows_hotel
    ON booking.hotel_workflow_approvals(hotel_id);
CREATE INDEX IF NOT EXISTS idx_hotel_workflows_change_request
    ON booking.hotel_workflow_approvals(change_request_id);
CREATE INDEX IF NOT EXISTS idx_hotel_workflows_process
    ON booking.hotel_workflow_approvals(process_instance_id);
CREATE INDEX IF NOT EXISTS idx_hotel_workflows_status
    ON booking.hotel_workflow_approvals(workflow_status);
