package com.booking.payment.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refund_history", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "gateway_refund_txn_id", length = 100)
    private String gatewayRefundTxnId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
