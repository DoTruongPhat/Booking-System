package com.booking.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface PaymentEvent {

    record PaymentInitiated(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, String paymentCode,
            BigDecimal amount, String method, String paymentUrl, Instant expiresAt
    ) implements PaymentEvent {
        public PaymentInitiated(UUID bookingId, UUID paymentId, String paymentCode,
                                BigDecimal amount, String method, String paymentUrl, Instant expiresAt) {
            this(UUID.randomUUID(), "PAYMENT_INITIATED", Instant.now(),
                    bookingId, paymentId, paymentCode, amount, method, paymentUrl, expiresAt);
        }
    }

    record PaymentSucceeded(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, String paymentCode,
            BigDecimal amount, String method, String gatewayTxnId, Instant completedAt
    ) implements PaymentEvent {
        public PaymentSucceeded(UUID bookingId, UUID paymentId, String paymentCode,
                                BigDecimal amount, String method, String gatewayTxnId) {
            this(UUID.randomUUID(), "PAYMENT_SUCCEEDED", Instant.now(),
                    bookingId, paymentId, paymentCode, amount, method, gatewayTxnId, Instant.now());
        }
    }

    record PaymentFailed(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, String reason, String gatewayResponse
    ) implements PaymentEvent {
        public PaymentFailed(UUID bookingId, UUID paymentId, String reason, String gatewayResponse) {
            this(UUID.randomUUID(), "PAYMENT_FAILED", Instant.now(),
                    bookingId, paymentId, reason, gatewayResponse);
        }
    }

    record PaymentExpired(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, Instant expiredAt
    ) implements PaymentEvent {
        public PaymentExpired(UUID bookingId, UUID paymentId) {
            this(UUID.randomUUID(), "PAYMENT_EXPIRED", Instant.now(),
                    bookingId, paymentId, Instant.now());
        }
    }

    record PaymentCancelled(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, String reason, UUID cancelledBy
    ) implements PaymentEvent {
        public PaymentCancelled(UUID bookingId, UUID paymentId, String reason, UUID cancelledBy) {
            this(UUID.randomUUID(), "PAYMENT_CANCELLED", Instant.now(),
                    bookingId, paymentId, reason, cancelledBy);
        }
    }

    record RefundCompleted(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, UUID refundId,
            BigDecimal amount, String gatewayRefundTxnId
    ) implements PaymentEvent {
        public RefundCompleted(UUID bookingId, UUID paymentId, UUID refundId,
                               BigDecimal amount, String gatewayRefundTxnId) {
            this(UUID.randomUUID(), "REFUND_COMPLETED", Instant.now(),
                    bookingId, paymentId, refundId, amount, gatewayRefundTxnId);
        }
    }

    record RefundFailed(
            UUID eventId, String eventType, Instant timestamp,
            UUID bookingId, UUID paymentId, UUID refundId, String reason
    ) implements PaymentEvent {
        public RefundFailed(UUID bookingId, UUID paymentId, UUID refundId, String reason) {
            this(UUID.randomUUID(), "REFUND_FAILED", Instant.now(),
                    bookingId, paymentId, refundId, reason);
        }
    }
}
