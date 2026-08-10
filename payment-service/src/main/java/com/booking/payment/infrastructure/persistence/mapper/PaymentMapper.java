package com.booking.payment.infrastructure.persistence.mapper;

import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.ProcessedEvent;
import com.booking.payment.domain.model.RefundHistory;
import com.booking.payment.domain.model.enums.PaymentMethod;
import com.booking.payment.domain.model.enums.PaymentStatus;
import com.booking.payment.domain.model.enums.RefundStatus;
import com.booking.payment.infrastructure.persistence.entity.PaymentEntity;
import com.booking.payment.infrastructure.persistence.entity.ProcessedEventEntity;
import com.booking.payment.infrastructure.persistence.entity.RefundHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    // ── Payment ─────────────────────────────

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) return null;

        Payment p = new Payment();
        p.setId(entity.getId());
        p.setPaymentCode(entity.getPaymentCode());
        p.setBookingId(entity.getBookingId());
        p.setUserId(entity.getUserId());
        p.setAmount(entity.getAmount());
        p.setCurrency(entity.getCurrency());
        p.setMethod(PaymentMethod.valueOf(entity.getMethod()));
        p.setStatus(PaymentStatus.valueOf(entity.getStatus()));
        p.setGatewayTxnId(entity.getGatewayTxnId());
        p.setGatewayUrl(entity.getGatewayUrl());
        p.setGatewayResponse(entity.getGatewayResponse());
        p.setIdempotencyKey(entity.getIdempotencyKey());
        p.setInitiatedAt(entity.getInitiatedAt());
        p.setCompletedAt(entity.getCompletedAt());
        p.setExpiresAt(entity.getExpiresAt());
        p.setMetadata(entity.getMetadata());
        p.setCreatedAt(entity.getCreatedAt());
        p.setUpdatedAt(entity.getUpdatedAt());
        return p;
    }

    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) return null;

        return PaymentEntity.builder()
                .id(domain.getId())
                .paymentCode(domain.getPaymentCode())
                .bookingId(domain.getBookingId())
                .userId(domain.getUserId())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .method(domain.getMethod().name())
                .status(domain.getStatus().name())
                .gatewayTxnId(domain.getGatewayTxnId())
                .gatewayUrl(domain.getGatewayUrl())
                .gatewayResponse(domain.getGatewayResponse())
                .idempotencyKey(domain.getIdempotencyKey())
                .initiatedAt(domain.getInitiatedAt())
                .completedAt(domain.getCompletedAt())
                .expiresAt(domain.getExpiresAt())
                .metadata(domain.getMetadata())
                .build();
    }

    public void updateEntity(PaymentEntity entity, Payment domain) {
        entity.setStatus(domain.getStatus().name());
        entity.setGatewayTxnId(domain.getGatewayTxnId());
        entity.setGatewayUrl(domain.getGatewayUrl());
        entity.setGatewayResponse(domain.getGatewayResponse());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setMetadata(domain.getMetadata());
    }

    // ── RefundHistory ───────────────────────

    public RefundHistory toDomain(RefundHistoryEntity entity) {
        if (entity == null) return null;

        RefundHistory r = new RefundHistory();
        r.setId(entity.getId());
        r.setPaymentId(entity.getPaymentId());
        r.setAmount(entity.getAmount());
        r.setReason(entity.getReason());
        r.setStatus(RefundStatus.valueOf(entity.getStatus()));
        r.setGatewayRefundTxnId(entity.getGatewayRefundTxnId());
        r.setIdempotencyKey(entity.getIdempotencyKey());
        r.setRequestedBy(entity.getRequestedBy());
        r.setRequestedAt(entity.getRequestedAt());
        r.setCompletedAt(entity.getCompletedAt());
        r.setMetadata(entity.getMetadata());
        return r;
    }

    public RefundHistoryEntity toEntity(RefundHistory domain) {
        if (domain == null) return null;

        return RefundHistoryEntity.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .amount(domain.getAmount())
                .reason(domain.getReason())
                .status(domain.getStatus().name())
                .gatewayRefundTxnId(domain.getGatewayRefundTxnId())
                .idempotencyKey(domain.getIdempotencyKey())
                .requestedBy(domain.getRequestedBy())
                .requestedAt(domain.getRequestedAt())
                .completedAt(domain.getCompletedAt())
                .metadata(domain.getMetadata())
                .build();
    }

    // ── ProcessedEvent ──────────────────────

    public ProcessedEvent toDomain(ProcessedEventEntity entity) {
        if (entity == null) return null;

        ProcessedEvent pe = new ProcessedEvent();
        pe.setEventType(entity.getEventType());
        pe.setEventId(entity.getEventId());
        pe.setProcessedAt(entity.getProcessedAt());
        return pe;
    }

    public ProcessedEventEntity toEntity(ProcessedEvent domain) {
        if (domain == null) return null;

        return ProcessedEventEntity.builder()
                .eventType(domain.getEventType())
                .eventId(domain.getEventId())
                .processedAt(domain.getProcessedAt())
                .build();
    }
}