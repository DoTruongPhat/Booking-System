package com.booking.payment.infrastructure.persistence.adapter;

import com.booking.payment.application.port.out.RefundRepositoryPort;
import com.booking.payment.domain.model.RefundHistory;
import com.booking.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.booking.payment.infrastructure.persistence.repository.RefundHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefundAdapter implements RefundRepositoryPort {

    private final RefundHistoryJpaRepository jpaRepository;
    private final PaymentMapper mapper;

    @Override
    public RefundHistory save(RefundHistory refund) {
        var entity = mapper.toEntity(refund);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefundHistory> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RefundHistory> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentIdOrderByRequestedAtDesc(paymentId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<RefundHistory> findByPaymentIdAndIdempotencyKey(UUID paymentId, String idempotencyKey) {
        return jpaRepository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public BigDecimal sumActiveRefunds(UUID paymentId) {
        return jpaRepository.sumActiveRefunds(paymentId);
    }
}