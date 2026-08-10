package com.booking.payment.infrastructure.persistence.repository;

import com.booking.payment.infrastructure.persistence.entity.RefundHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundHistoryJpaRepository extends JpaRepository<RefundHistoryEntity, UUID> {

    List<RefundHistoryEntity> findByPaymentIdOrderByRequestedAtDesc(UUID paymentId);

    /** Check idempotency: same payment + same key */
    Optional<RefundHistoryEntity> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey);

    /**
     * Point 8: Sum all PENDING + SUCCESS refunds for a payment.
     * Used with pessimistic lock on payment row to prevent exceeding amount.
     */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM RefundHistoryEntity r " +
            "WHERE r.paymentId = :paymentId AND r.status IN ('PENDING', 'PROCESSING', 'SUCCESS')")
    BigDecimal sumActiveRefunds(@Param("paymentId") UUID paymentId);
}