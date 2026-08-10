package com.booking.payment.application.service;

import com.booking.payment.application.port.in.VerifyCallbackUseCase;
import com.booking.payment.application.port.out.*;
import com.booking.payment.domain.event.PaymentEvent;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.ProcessedEvent;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import com.booking.payment.infrastructure.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VerifyCallbackService implements VerifyCallbackUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final ProcessedEventRepositoryPort processedEventRepository;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentMetrics paymentMetrics;

    private static final String EVENT_TYPE = "GATEWAY_CALLBACK";

    @Override
    public void execute(String gatewayName, Map<String, String> params) {

        // 1. Verify via gateway
        PaymentGatewayPort gateway = gatewayFactory.getGateway(gatewayName);
        var result = gateway.verifyCallback(params);

        // 2. Signature check
        if (!result.signatureValid()) {
            log.warn("SECURITY: Invalid signature from {}: params={}", gatewayName, params);
            throw new PaymentException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        // 3. Idempotency check (composite: eventType + eventId)
        if (processedEventRepository.exists(EVENT_TYPE, result.gatewayTxnId())) {
            log.info("Callback already processed: type={}, txn={}", EVENT_TYPE, result.gatewayTxnId());
            return;
        }

        // 4. Find payment
        Payment payment = paymentRepository.findByGatewayTxnId(result.gatewayTxnId())
                .orElseThrow(() -> {
                    log.error("Payment not found for gateway txn: {}", result.gatewayTxnId());
                    return new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND,
                            "Gateway txn: " + result.gatewayTxnId());
                });

        // 5. Amount check — CRITICAL security
        if (result.amount() != null && payment.getAmount().compareTo(result.amount()) != 0) {
            log.error("CRITICAL: Amount mismatch! Payment={}, Callback={}. Possible tampering.",
                    payment.getAmount(), result.amount());
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 6. Status check — only PENDING or EXPIRED
        if (payment.isExpired()) {
            if (!result.success()) {
                log.warn("Payment {} is EXPIRED, ignoring non-success late callback from {}",
                        payment.getId(), gatewayName);
                processedEventRepository.save(new ProcessedEvent(EVENT_TYPE, result.gatewayTxnId()));
                return;
            }
            log.warn("Payment {} is EXPIRED locally but gateway reports success, reconciling",
                    payment.getId());
        }
        if (!payment.isPending() && !payment.isExpired()) {
            log.warn("Payment {} already {}, ignoring callback", payment.getId(), payment.getStatus());
            return;
        }

        // 7. Update payment
        if (result.success()) {
            payment.markSuccess(result.gatewayTxnId(), result.rawResponse());
            log.info("Payment SUCCESS: code={}, booking={}", payment.getPaymentCode(), payment.getBookingId());
            paymentMetrics.recordSuccess(payment.getMethod().name());

        } else {
            payment.markFailed(result.rawResponse());
            log.info("Payment FAILED: code={}, booking={}, responseCode={}",
                    payment.getPaymentCode(), payment.getBookingId(), result.responseCode());
            paymentMetrics.recordFailed(result.responseCode());
        }

        paymentRepository.save(payment);

        // 8. Mark processed
        processedEventRepository.save(new ProcessedEvent(EVENT_TYPE, result.gatewayTxnId()));

        // 9. Publish Kafka event
        if (result.success()) {
            eventPublisher.publish(new PaymentEvent.PaymentSucceeded(
                    payment.getBookingId(), payment.getId(), payment.getPaymentCode(),
                    payment.getAmount(), payment.getMethod().name(), payment.getGatewayTxnId()));
        } else {
            eventPublisher.publish(new PaymentEvent.PaymentFailed(
                    payment.getBookingId(), payment.getId(),
                    "Gateway response: " + result.responseCode(), result.rawResponse()));
        }
    }
}
