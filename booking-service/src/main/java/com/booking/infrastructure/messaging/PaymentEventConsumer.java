package com.booking.infrastructure.messaging;

import com.booking.application.port.in.PaymentBookingSyncUseCase;
import com.booking.domain.enums.CancelledBy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentBookingSyncUseCase paymentBookingSyncUseCase;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_PREFIX = "payment-event:processed:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);

    @KafkaListener(
            topics = "${app.kafka.topic.payment-events:payment-events}",
            groupId = "core-payment-listener"
    )
    public void onPaymentEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = text(node, "eventType", "");

            switch (eventType) {
                case "PAYMENT_SUCCEEDED" -> processOnce(node, () -> handlePaymentSucceeded(node));
                case "PAYMENT_EXPIRED" -> processOnce(node, () -> handlePaymentExpired(node));
                case "PAYMENT_FAILED" -> processOnce(node, () -> handlePaymentFailed(node));
                case "PAYMENT_CANCELLED" -> processOnce(node, () -> handlePaymentCancelled(node));
                case "REFUND_COMPLETED" -> processOnce(node, () -> handleRefundCompleted(node));
                default -> log.debug("Ignoring payment event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to process payment event", e);
        }
    }

    private void processOnce(JsonNode node, Runnable action) {
        String eventId = requiredText(node, "eventId");
        String redisKey = IDEMPOTENCY_PREFIX + eventId;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            log.info("Payment event already processed: {}", eventId);
            return;
        }

        action.run();
        redisTemplate.opsForValue().set(redisKey, "1", IDEMPOTENCY_TTL);
    }

    private void handlePaymentSucceeded(JsonNode node) {
        UUID bookingId = requiredUuid(node, "bookingId");
        UUID paymentId = requiredUuid(node, "paymentId");
        BigDecimal amount = requiredDecimal(node, "amount");
        String method = text(node, "method", "ONLINE");
        Instant completedAt = instant(node, "completedAt", Instant.now());

        paymentBookingSyncUseCase.markPaidAndConfirm(bookingId, paymentId, amount, method, completedAt);
        log.info("Processed PaymentSucceeded: bookingId={}, paymentId={}", bookingId, paymentId);
    }

    private void handlePaymentExpired(JsonNode node) {
        UUID bookingId = requiredUuid(node, "bookingId");
        paymentBookingSyncUseCase.cancelForPaymentEvent(
                bookingId,
                CancelledBy.SYSTEM,
                "Payment expired"
        );
        log.info("Processed PaymentExpired: bookingId={}", bookingId);
    }

    private void handlePaymentFailed(JsonNode node) {
        UUID bookingId = requiredUuid(node, "bookingId");
        String reason = text(node, "reason", "Payment failed");
        log.info("Processed PaymentFailed without cancelling booking: bookingId={}, reason={}",
                bookingId, reason);
    }

    private void handlePaymentCancelled(JsonNode node) {
        UUID bookingId = requiredUuid(node, "bookingId");
        String reason = text(node, "reason", "Payment cancelled by user");
        log.info("Processed PaymentCancelled without cancelling booking: bookingId={}, reason={}",
                bookingId, reason);
    }

    private void handleRefundCompleted(JsonNode node) {
        UUID bookingId = requiredUuid(node, "bookingId");
        UUID paymentId = requiredUuid(node, "paymentId");
        BigDecimal amount = requiredDecimal(node, "amount");
        paymentBookingSyncUseCase.applyPaymentRefund(bookingId, paymentId, amount);
        log.info("Processed RefundCompleted: bookingId={}, paymentId={}, amount={}",
                bookingId, paymentId, amount);
    }

    private String requiredText(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            throw new IllegalArgumentException("Payment event missing field: " + field);
        }
        return node.get(field).asText();
    }

    private String text(JsonNode node, String field, String defaultValue) {
        return node.hasNonNull(field) ? node.get(field).asText() : defaultValue;
    }

    private UUID requiredUuid(JsonNode node, String field) {
        return UUID.fromString(requiredText(node, field));
    }

    private BigDecimal requiredDecimal(JsonNode node, String field) {
        return new BigDecimal(requiredText(node, field));
    }

    private Instant instant(JsonNode node, String field, Instant defaultValue) {
        return node.hasNonNull(field) ? Instant.parse(node.get(field).asText()) : defaultValue;
    }
}
