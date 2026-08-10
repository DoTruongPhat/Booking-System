package com.booking.payment.application.service;

import com.booking.payment.application.port.in.InitRefundUseCase;
import com.booking.payment.application.port.out.*;
import com.booking.payment.domain.event.PaymentEvent;
import com.booking.payment.domain.exception.PaymentErrorCode;
import com.booking.payment.domain.exception.PaymentException;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.RefundHistory;
import com.booking.payment.infrastructure.gateway.PaymentGatewayFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InitRefundService implements InitRefundUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final RefundRepositoryPort refundRepository;
    private final PaymentEventPublisherPort eventPublisher;
    private final PaymentGatewayFactory gatewayFactory;

    @Override
    public RefundHistory execute(UUID paymentId, BigDecimal amount,
                                 String reason, UUID requestedBy, String idempotencyKey) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_REQUEST, "Refund amount must be greater than 0");
        }

        // 1. Idempotency check (DB level)
        if (idempotencyKey != null) {
            var existing = refundRepository.findByPaymentIdAndIdempotencyKey(paymentId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Refund idempotent hit: paymentId={}, key={}", paymentId, idempotencyKey);
                return existing.get();
            }
        }

        // 2. Pessimistic lock on payment row (Point 8)
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        // 3. Status check
        if (!payment.canRefund()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_NOT_SUCCESS,
                    "Current status: " + payment.getStatus());
        }

        // 4. Amount check — sum from refund_history (Point 8)
        BigDecimal totalRefunded = refundRepository.sumActiveRefunds(paymentId);
        BigDecimal remaining = payment.getAmount().subtract(totalRefunded);

        if (amount.compareTo(remaining) > 0) {
            throw new PaymentException(PaymentErrorCode.REFUND_EXCEEDS_AMOUNT,
                    "Requested: " + amount + ", Remaining: " + remaining
                            + " (Already refunded: " + totalRefunded + ")");
        }

        // 5. Create refund record
        RefundHistory refund = new RefundHistory();
        refund.setPaymentId(paymentId);
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setRequestedBy(requestedBy);
        refund.setIdempotencyKey(idempotencyKey);
        refund.markProcessing();

        RefundHistory savedRefund = refundRepository.save(refund);

        // 6. Call gateway
        try {
            var gateway = gatewayFactory.getGateway(payment.getMethod());
            var result = gateway.refund(new PaymentGatewayPort.RefundRequest(
                    payment.getGatewayTxnId(), amount, reason));

            if (result.success()) {
                savedRefund.markSuccess(result.gatewayRefundTxnId());
                savedRefund = refundRepository.save(savedRefund);

                // 7. Update payment status based on total refunded
                BigDecimal newTotal = totalRefunded.add(amount);
                payment.updateRefundStatus(newTotal);
                paymentRepository.save(payment);

                eventPublisher.publish(new PaymentEvent.RefundCompleted(
                        payment.getBookingId(), payment.getId(), savedRefund.getId(),
                        amount, result.gatewayRefundTxnId()));

                log.info("Refund success: paymentCode={}, amount={}, refundId={}",
                        payment.getPaymentCode(), amount, savedRefund.getId());

                return savedRefund;

            } else {
                savedRefund.markFailed();
                savedRefund = refundRepository.save(savedRefund);

                eventPublisher.publish(new PaymentEvent.RefundFailed(
                        payment.getBookingId(), payment.getId(), savedRefund.getId(),
                        "Gateway refund failed"));

                log.error("Refund failed: paymentCode={}, amount={}", payment.getPaymentCode(), amount);
                throw new PaymentException(PaymentErrorCode.REFUND_FAILED);
            }

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            savedRefund.markFailed();
            refundRepository.save(savedRefund);
            log.error("Refund error: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.REFUND_FAILED, e.getMessage());
        }
    }
}
