package com.booking.payment.application.service;

import com.booking.payment.application.port.in.CheckExpiredPaymentsUseCase;
import com.booking.payment.application.port.out.PaymentEventPublisherPort;
import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.domain.event.PaymentEvent;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import com.booking.payment.infrastructure.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckExpiredPaymentsScheduler implements CheckExpiredPaymentsUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentMetrics paymentMetrics;

    @Override
    @Scheduled(fixedDelayString = "${app.payment.expire-check-interval:60000}")
    @Transactional
    public void execute() {
        List<Payment> expired = paymentRepository.findExpiredPending();
        if (expired.isEmpty()) return;

        log.info("Found {} expired PENDING payments", expired.size());

        for (Payment payment : expired) {
            try {
                // Best effort cancel on gateway
                if (payment.getGatewayTxnId() != null) {
                    try {
                        var gateway = gatewayFactory.getGateway(payment.getMethod());
                        gateway.cancelPayment(payment.getGatewayTxnId());
                    } catch (Exception e) {
                        log.warn("Gateway cancel failed for {}: {}", payment.getPaymentCode(), e.getMessage());
                    }
                }

                payment.markExpired();
                paymentMetrics.recordExpired();
                paymentRepository.save(payment);

                eventPublisher.publish(new PaymentEvent.PaymentExpired(
                        payment.getBookingId(), payment.getId()));

                log.info("Payment expired: code={}, booking={}",
                        payment.getPaymentCode(), payment.getBookingId());

            } catch (Exception e) {
                log.error("Failed to expire payment {}: {}", payment.getPaymentCode(), e.getMessage());
            }
        }
    }
}