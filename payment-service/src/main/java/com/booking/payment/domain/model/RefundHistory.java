package com.booking.payment.domain.model;

import com.booking.payment.domain.model.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RefundHistory {

    private UUID id;
    private UUID paymentId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private String gatewayRefundTxnId;
    private String idempotencyKey;
    private UUID requestedBy;
    private Instant requestedAt;
    private Instant completedAt;
    private String metadata;

    public RefundHistory() {
        this.status = RefundStatus.PENDING;
        this.requestedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = RefundStatus.PROCESSING;
    }

    public void markSuccess(String gatewayRefundTxnId) {
        this.status = RefundStatus.SUCCESS;
        this.gatewayRefundTxnId = gatewayRefundTxnId;
        this.completedAt = Instant.now();
    }

    public void markFailed() {
        this.status = RefundStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public RefundStatus getStatus() { return status; }
    public void setStatus(RefundStatus status) { this.status = status; }

    public String getGatewayRefundTxnId() { return gatewayRefundTxnId; }
    public void setGatewayRefundTxnId(String gatewayRefundTxnId) { this.gatewayRefundTxnId = gatewayRefundTxnId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public UUID getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UUID requestedBy) { this.requestedBy = requestedBy; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}