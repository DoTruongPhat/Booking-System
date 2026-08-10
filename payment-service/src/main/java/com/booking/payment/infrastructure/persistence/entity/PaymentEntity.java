package com.booking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_code", nullable = false, unique = true, length = 50)
    private String paymentCode;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "VND";

    @Column(nullable = false, length = 20)
    private String method;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "gateway_txn_id", length = 100)
    private String gatewayTxnId;

    @Column(name = "gateway_url", columnDefinition = "TEXT")
    private String gatewayUrl;

    @Column(name = "gateway_response", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String gatewayResponse;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (initiatedAt == null) initiatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
