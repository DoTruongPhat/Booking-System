package com.booking.payment.domain.model;

import com.booking.payment.domain.model.enums.PaymentMethod;
import com.booking.payment.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Payment {

    private UUID id;
    private String paymentCode;
    private UUID bookingId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private PaymentStatus status;
    private String gatewayTxnId;
    private String gatewayUrl;
    private String gatewayResponse;
    private String idempotencyKey;
    private Instant initiatedAt;
    private Instant completedAt;
    private Instant expiresAt;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public Payment() {
        this.currency = "VND";
        this.status = PaymentStatus.PENDING;
    }

    // ── Domain logic ────────────────────────

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }

    public boolean isExpired() {
        return this.status == PaymentStatus.EXPIRED;
    }

    public boolean canCancel() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean canRefund() {
        return this.status == PaymentStatus.SUCCESS
                || this.status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    public boolean isExpiredByTime() {
        return this.status == PaymentStatus.PENDING
                && this.expiresAt != null
                && Instant.now().isAfter(this.expiresAt);
    }

    public void markSuccess(String gatewayTxnId, String gatewayResponse) {
        this.status = PaymentStatus.SUCCESS;
        this.gatewayTxnId = gatewayTxnId;
        this.gatewayResponse = normalizeJson(gatewayResponse);
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markFailed(String gatewayResponse) {
        this.status = PaymentStatus.FAILED;
        this.gatewayResponse = normalizeJson(gatewayResponse);
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        this.status = PaymentStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /**
     * Update payment status based on total refunded from refund_history.
     * Called after each successful refund.
     */
    public void updateRefundStatus(BigDecimal totalRefunded) {
        if (totalRefunded.compareTo(this.amount) >= 0) {
            this.status = PaymentStatus.REFUNDED;
        } else if (totalRefunded.compareTo(BigDecimal.ZERO) > 0) {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
        this.updatedAt = Instant.now();
    }

    // ── Getters / Setters ───────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPaymentCode() { return paymentCode; }
    public void setPaymentCode(String paymentCode) { this.paymentCode = paymentCode; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getGatewayTxnId() { return gatewayTxnId; }
    public void setGatewayTxnId(String gatewayTxnId) { this.gatewayTxnId = gatewayTxnId; }

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if ((value.startsWith("{") && value.contains("\""))
                || value.startsWith("[")
                || value.startsWith("\"")) {
            return value;
        }
        return "{\"raw\":\"" + escapeJson(value) + "\"}";
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
