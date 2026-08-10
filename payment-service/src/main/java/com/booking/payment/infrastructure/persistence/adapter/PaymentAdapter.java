package com.booking.payment.infrastructure.persistence.adapter;

import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.infrastructure.persistence.entity.PaymentEntity;
import com.booking.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.booking.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity;

        if (payment.getId() != null) {
            entity = jpaRepository.findById(payment.getId()).orElse(null);
            if (entity != null) {
                mapper.updateEntity(entity, payment);
            } else {
                entity = mapper.toEntity(payment);
            }
        } else {
            entity = mapper.toEntity(payment);
        }

        PaymentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findActiveByBookingId(UUID bookingId) {
        return jpaRepository.findFirstByBookingIdAndStatusInOrderByCreatedAtDesc(
                bookingId,
                List.of("PENDING", "PROCESSING")).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findSuccessfulByBookingId(UUID bookingId) {
        return jpaRepository.findSuccessfulByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findAllByBookingId(UUID bookingId) {
        return jpaRepository.findAllByBookingId(bookingId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Payment> findByGatewayTxnId(String gatewayTxnId) {
        return jpaRepository.findByGatewayTxnId(gatewayTxnId).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Page<Payment> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Payment> findAll(String status, Pageable pageable) {
        return jpaRepository.findAllByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Payment> findExpiredPending() {
        return jpaRepository.findExpiredPending(Instant.now()).stream()
                .map(mapper::toDomain).toList();
    }
}
