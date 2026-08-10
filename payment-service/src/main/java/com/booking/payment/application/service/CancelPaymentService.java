package com.booking.payment.application.service;

import com.booking.payment.application.port.in.CancelPaymentUseCase;
import com.booking.payment.application.port.out.PaymentEventPublisherPort;
import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.domain.event.PaymentEvent;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CancelPaymentService implements CancelPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentGatewayFactory gatewayFactory;

    @Override
    public Payment execute(UUID paymentId, UUID userId, String reason) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_OWNED);
        }

        if (!payment.canCancel()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_PENDING,
                    "Current status: " + payment.getStatus());
        }

        // Best effort cancel on gateway
        try {
            if (payment.getGatewayTxnId() != null) {
                var gateway = gatewayFactory.getGateway(payment.getMethod());
                gateway.cancelPayment(payment.getGatewayTxnId());
            }
        } catch (Exception e) {
            log.warn("Gateway cancel failed (best effort): {}", e.getMessage());
        }

        payment.markCancelled();
        Payment saved = paymentRepository.save(payment);

        eventPublisher.publish(new PaymentEvent.PaymentCancelled(
                saved.getBookingId(), saved.getId(), reason, userId));

        log.info("Payment cancelled: code={}, booking={}, by={}",
                saved.getPaymentCode(), saved.getBookingId(), userId);

        return saved;
    }
}