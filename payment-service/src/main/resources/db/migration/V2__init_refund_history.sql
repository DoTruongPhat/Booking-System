CREATE TABLE payment.refund_history (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        payment_id UUID NOT NULL
                                            REFERENCES payment.payments(id)
                                                ON DELETE RESTRICT,
                                        amount DECIMAL(12,2) NOT NULL,
                                        reason TEXT,
                                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                        gateway_refund_txn_id VARCHAR(100),
                                        idempotency_key VARCHAR(100),
                                        requested_by UUID NOT NULL,
                                        requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        completed_at TIMESTAMPTZ,
                                        metadata JSONB,

                                        CONSTRAINT chk_refund_status CHECK (
                                            status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')
                                            ),
                                        CONSTRAINT chk_refund_amount_positive CHECK (
                                            amount > 0
                                            )
);

CREATE INDEX idx_refund_payment_requested
    ON payment.refund_history (payment_id, requested_at DESC);

CREATE INDEX idx_refund_requested_by
    ON payment.refund_history (requested_by, requested_at DESC);

CREATE UNIQUE INDEX idx_refund_payment_gateway_txn
    ON payment.refund_history (payment_id, gateway_refund_txn_id)
    WHERE gateway_refund_txn_id IS NOT NULL;

CREATE UNIQUE INDEX idx_refund_payment_idempotency
    ON payment.refund_history (payment_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;