package com.booking.payment.application.service;

import com.booking.payment.application.port.in.InitPaymentUseCase;
import com.booking.payment.application.port.out.*;
import com.booking.payment.domain.event.PaymentEvent;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.enums.PaymentMethod;
import com.booking.payment.domain.model.enums.PaymentStatus;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import com.booking.payment.infrastructure.metrics.PaymentMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InitPaymentService implements InitPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentCachePort paymentCache;
    private final PaymentEventPublisherPort eventPublisher;
    private final BookingPaymentValidationPort bookingPaymentValidationPort;
    private final PaymentGatewayFactory gatewayFactory;
    private final PaymentMetrics paymentMetrics;

    @Value("${app.payment.expiration-minutes:30}")
    private int expirationMinutes;

    @Override
    public InitPaymentResult execute(InitPaymentCommand command) {

        // 1. Idempotency check (Redis)
        if (command.idempotencyKey() != null) {
            var cached = paymentCache.getByIdempotencyKey(command.idempotencyKey());
            if (cached.isPresent()) {
                var existing = paymentRepository.findById(UUID.fromString(cached.get()));
                if (existing.isPresent()) {
                    Payment p = existing.get();
                    log.info("Idempotent hit: key={}, paymentId={}", command.idempotencyKey(), p.getId());
                    return new InitPaymentResult(p.getId(), p.getPaymentCode(), p.getGatewayUrl(), p.getExpiresAt());
                }
            }
        }

        // 2. Validate booking snapshot from booking-service. Never trust client amount.
        var booking = bookingPaymentValidationPort.getPaymentSnapshot(command.bookingId());
        if (!booking.userId().equals(command.userId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_OWNED);
        }
        if (!"PENDING".equals(booking.status())) {
            throw new PaymentException(PaymentErrorCode.BOOKING_NOT_PAYABLE,
                    "Booking status is " + booking.status());
        }
        if (!"UNPAID".equals(booking.paymentStatus())) {
            throw new PaymentException(PaymentErrorCode.BOOKING_NOT_PAYABLE,
                    "Booking payment status is " + booking.paymentStatus());
        }
        if (command.amount() != null && booking.totalPrice().compareTo(command.amount()) != 0) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH,
                    "Expected " + booking.totalPrice() + ", got " + command.amount());
        }

        // 3. Resolve gateway
        PaymentMethod method = resolveMethod(command.method());
        PaymentGatewayPort gateway = gatewayFactory.getGateway(method);

        // 4. Reuse active QR/link when it is still payable; close stale ones locally.
        var activePayment = paymentRepository.findActiveByBookingId(command.bookingId());
        if (activePayment.isPresent()) {
            Payment p = activePayment.get();
            if (!p.getUserId().equals(command.userId())) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_OWNED);
            }
            if (p.getStatus() == PaymentStatus.PROCESSING) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_EXISTS,
                        "Payment " + p.getId() + " is processing");
            }
            PaymentGatewayPort.GatewayPaymentStatus gatewayStatus = p.getGatewayTxnId() != null
                    ? gateway.getPaymentStatus(p.getGatewayTxnId())
                    : PaymentGatewayPort.GatewayPaymentStatus.NOT_FOUND;
            if (gatewayStatus == PaymentGatewayPort.GatewayPaymentStatus.PAID) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_EXISTS,
                        "Payment " + p.getId() + " is already paid at gateway and is waiting confirmation");
            }
            if (isReusablePayment(p, method, gatewayStatus)) {
                cacheIdempotency(command.idempotencyKey(), p);
                log.info("Reusing active payment: code={}, booking={}, method={}",
                        p.getPaymentCode(), command.bookingId(), method);
                return new InitPaymentResult(p.getId(), p.getPaymentCode(), p.getGatewayUrl(), p.getExpiresAt());
            }
            closeStaleActivePayment(p);
        }

        // 5. Also check if booking already paid successfully
        var successPayment = paymentRepository.findSuccessfulByBookingId(command.bookingId());
        if (successPayment.isPresent()) {
            Payment p = successPayment.get();
            if (p.getStatus() == PaymentStatus.SUCCESS) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_ALREADY_EXISTS,
                        "Booking " + command.bookingId() + " already paid: " + p.getId());
            }
        }

        // 6. Build payment
        Instant now = Instant.now();
        Instant configuredFallback = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        Instant expiresAt = booking.paymentExpiresAt() != null
                ? booking.paymentExpiresAt()
                : configuredFallback;
        if (!expiresAt.isAfter(now)) {
            expiresAt = configuredFallback;
        }
        var payableAmount = booking.totalPrice();

        Payment payment = new Payment();
        payment.setPaymentCode(generatePaymentCode());
        payment.setBookingId(command.bookingId());
        payment.setUserId(command.userId());
        payment.setAmount(payableAmount);
        payment.setCurrency(command.currency() != null ? command.currency() : booking.currency());
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(command.idempotencyKey());
        payment.setInitiatedAt(now);
        payment.setExpiresAt(expiresAt);

        // 7. Save first (get ID for gateway)
        Payment saved = paymentRepository.save(payment);
        paymentMetrics.recordInitiated(method.name());

        // 8. Init gateway
        try {
            var gatewayResult = gateway.initPayment(new PaymentGatewayPort.InitPaymentRequest(
                    saved.getId(), command.bookingId(), payableAmount,
                    saved.getCurrency(), "Booking " + command.bookingId(),
                    null, null
            ));

            if (!gatewayResult.success() || gatewayResult.paymentUrl() == null || gatewayResult.paymentUrl().isBlank()) {
                throw new IllegalStateException(gatewayResult.rawResponse());
            }

            saved.setGatewayTxnId(gatewayResult.gatewayTxnId());
            saved.setGatewayUrl(gatewayResult.paymentUrl());
            saved.setGatewayResponse(gatewayResult.rawResponse());
            saved = paymentRepository.save(saved);

        } catch (Exception e) {
            saved.markFailed(e.getMessage());
            paymentRepository.save(saved);
            log.error("Gateway init failed for booking {}: {}", command.bookingId(), e.getMessage());
            throw new PaymentException(PaymentErrorCode.PAYMENT_GATEWAY_ERROR, e.getMessage());
        }

        // 9. Cache idempotency
        cacheIdempotency(command.idempotencyKey(), saved);

        // 10. Publish event
        eventPublisher.publish(new PaymentEvent.PaymentInitiated(
                saved.getBookingId(), saved.getId(), saved.getPaymentCode(),
                saved.getAmount(), saved.getMethod().name(),
                saved.getGatewayUrl(), saved.getExpiresAt()));

        log.info("Payment initiated: code={}, booking={}, method={}, expiresAt={}",
                saved.getPaymentCode(), command.bookingId(), method, expiresAt);

        return new InitPaymentResult(saved.getId(), saved.getPaymentCode(),
                saved.getGatewayUrl(), saved.getExpiresAt());
    }

    private String generatePaymentCode() {
        String prefix = "PAY-" + LocalDate.now().toString().replace("-", "");
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PaymentMethod resolveMethod(String rawMethod) {
        try {
            return PaymentMethod.valueOf(rawMethod.toUpperCase());
        } catch (Exception e) {
            throw new PaymentException(PaymentErrorCode.GATEWAY_NOT_SUPPORTED,
                    "Unsupported payment method: " + rawMethod);
        }
    }

    private boolean isReusablePayment(
            Payment payment,
            PaymentMethod requestedMethod,
            PaymentGatewayPort.GatewayPaymentStatus gatewayStatus
    ) {
        return payment.getStatus() == PaymentStatus.PENDING
                && payment.getMethod() == requestedMethod
                && payment.getGatewayUrl() != null
                && !payment.getGatewayUrl().isBlank()
                && payment.getExpiresAt() != null
                && payment.getExpiresAt().isAfter(Instant.now())
                && gatewayStatus != PaymentGatewayPort.GatewayPaymentStatus.CANCELLED
                && gatewayStatus != PaymentGatewayPort.GatewayPaymentStatus.EXPIRED
                && gatewayStatus != PaymentGatewayPort.GatewayPaymentStatus.FAILED
                && gatewayStatus != PaymentGatewayPort.GatewayPaymentStatus.NOT_FOUND;
    }

    private void closeStaleActivePayment(Payment payment) {
        if (payment.getGatewayTxnId() != null && !payment.getGatewayTxnId().isBlank()) {
            try {
                gatewayFactory.getGateway(payment.getMethod()).cancelPayment(payment.getGatewayTxnId());
            } catch (Exception e) {
                log.warn("Gateway cancel failed while replacing payment {}: {}",
                        payment.getId(), e.getMessage());
            }
        }

        if (payment.isExpiredByTime()) {
            payment.markExpired();
        } else {
            payment.markCancelled();
        }
        paymentRepository.save(payment);
        log.info("Closed stale active payment before retry: code={}, status={}",
                payment.getPaymentCode(), payment.getStatus());
    }

    private void cacheIdempotency(String idempotencyKey, Payment payment) {
        if (idempotencyKey != null) {
            paymentCache.setIdempotencyKey(idempotencyKey, payment.getId().toString());
        }
    }
}
