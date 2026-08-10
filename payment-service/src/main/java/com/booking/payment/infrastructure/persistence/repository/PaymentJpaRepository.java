package com.booking.payment.infrastructure.persistence.repository;

import com.booking.payment.infrastructure.persistence.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT p FROM PaymentEntity p WHERE p.method = :method AND p.gatewayTxnId = :gatewayTxnId")
    Optional<PaymentEntity> findByMethodAndGatewayTxnId(
            @Param("method") String method,
            @Param("gatewayTxnId") String gatewayTxnId);

    /** Find by gateway txn id (any method) */
    Optional<PaymentEntity> findByGatewayTxnId(String gatewayTxnId);

    /** Active payment = PENDING or PROCESSING */
    Optional<PaymentEntity> findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(
            UUID bookingId,
            List<String> statuses);

    /** Find SUCCESS payment for booking (for refund) */
    @Query("SELECT p FROM PaymentEntity p WHERE p.bookingId = :bookingId " +
            "AND p.status IN ('SUCCESS', 'PARTIALLY_REFUNDED') " +
            "ORDER BY p.createdAt DESC")
    Optional<PaymentEntity> findSuccessfulByBookingId(@Param("bookingId") UUID bookingId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.bookingId = :bookingId " +
            "ORDER BY p.createdAt DESC")
    List<PaymentEntity> findAllByBookingId(@Param("bookingId") UUID bookingId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    Page<PaymentEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM PaymentEntity p WHERE p.status = 'PENDING' AND p.expiresAt < :now")
    List<PaymentEntity> findExpiredPending(@Param("now") Instant now);

    @Query("SELECT p FROM PaymentEntity p WHERE " +
            "(:status IS NULL OR p.status = :status) " +
            "ORDER BY p.createdAt DESC")
    Page<PaymentEntity> findAllByStatus(@Param("status") String status, Pageable pageable);

    /** Pessimistic lock for refund amount validation (Point 8) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id")
    Optional<PaymentEntity> findByIdForUpdate(@Param("id") UUID id);
}
