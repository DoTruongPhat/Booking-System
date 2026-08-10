CREATE SCHEMA IF NOT EXISTS payment;

CREATE TABLE payment.payments (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  payment_code VARCHAR(50) NOT NULL UNIQUE,
                                  booking_id UUID NOT NULL,
                                  user_id UUID NOT NULL,
                                  amount DECIMAL(12,2) NOT NULL,
                                  currency VARCHAR(3) NOT NULL DEFAULT 'VND',
                                  method VARCHAR(20) NOT NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                  gateway_txn_id VARCHAR(100),
                                  gateway_url TEXT,
                                  gateway_response JSONB,
                                  idempotency_key VARCHAR(100),
                                  initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  completed_at TIMESTAMPTZ,
                                  expires_at TIMESTAMPTZ NOT NULL,
                                  metadata JSONB,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                  CONSTRAINT chk_payment_status CHECK (status IN (
                                                                                  'PENDING', 'PROCESSING', 'SUCCESS', 'FAILED',
                                                                                  'CANCELLED', 'EXPIRED', 'REFUNDED', 'PARTIALLY_REFUNDED'
                                      )),
                                  CONSTRAINT chk_payment_method CHECK (method IN ('VNPAY', 'VIETQR')),
                                  CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

-- Point 7: chi chặn duplicate khi đang xử lý (PENDING/PROCESSING)
-- Booking đã REFUNDED vẫn tạo payment mới được
CREATE UNIQUE INDEX idx_payments_booking_active ON payment.payments (booking_id)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_payments_user_created ON payment.payments (user_id, created_at DESC);

CREATE INDEX idx_payments_status_expires ON payment.payments (status, expires_at)
    WHERE status = 'PENDING';

-- Point 5: composite (method, gateway_txn_id) tránh trùng cross-gateway
CREATE UNIQUE INDEX idx_payments_gateway_txn ON payment.payments (method, gateway_txn_id)
    WHERE gateway_txn_id IS NOT NULL;

CREATE UNIQUE INDEX idx_payments_idempotency ON payment.payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Point 4: composite PK (event_type, event_id) tránh conflict cross event types
CREATE TABLE payment.processed_events (
                                          event_id VARCHAR(100) NOT NULL,
                                          event_type VARCHAR(50) NOT NULL,
                                          processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                                          PRIMARY KEY (event_type, event_id)
);