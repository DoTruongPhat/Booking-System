package com.booking.payment.infrastructure.messaging;

import com.booking.payment.application.port.in.CancelPaymentUseCase;
import com.booking.payment.application.port.in.InitRefundUseCase;
import com.booking.payment.application.port.out.PaymentRepositoryPort;
import com.booking.payment.application.port.out.ProcessedEventRepositoryPort;
import com.booking.payment.domain.model.Payment;
import com.booking.payment.domain.model.ProcessedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventListener {

    private final PaymentRepositoryPort paymentRepository;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final InitRefundUseCase initRefundUseCase;
    private final ProcessedEventRepositoryPort processedEventRepository;
    private final ObjectMapper objectMapper;

    private static final String EVENT_TYPE = "CORE_EVENT";

    @KafkaListener(
            topics = "${app.kafka.topic.core-events:core-events}",
            groupId = "payment-core-listener"
    )
    public void onCoreEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            // Detect event type from JSON fields
            if (node.has("cancelType") || node.has("cancelledBy")) {
                handleBookingCancelled(node);
            } else {
                log.debug("Ignoring core event: {}", message);
            }

        } catch (Exception e) {
            log.error("Failed to process core event: {}", e.getMessage(), e);
        }
    }

    private void handleBookingCancelled(JsonNode node) {
        String bookingIdStr = node.has("bookingId") ? node.get("bookingId").asText() : null;
        if (bookingIdStr == null) {
            log.warn("BookingCancelled event missing bookingId");
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);

        // Idempotency check
        String eventId = "booking-cancelled:" + bookingId;
        if (processedEventRepository.exists(EVENT_TYPE, eventId)) {
            log.info("BookingCancelled already processed: {}", bookingId);
            return;
        }

        log.info("Processing BookingCancelled: bookingId={}", bookingId);

        // Find active payment for this booking
        var activePayment = paymentRepository.findActiveByBookingId(bookingId);
        if (activePayment.isPresent()) {
            // Payment still PENDING → cancel it
            Payment payment = activePayment.get();
            try {
                cancelPaymentUseCase.execute(payment.getId(), payment.getUserId(), "Booking cancelled");
                log.info("Payment cancelled for booking {}: paymentId={}", bookingId, payment.getId());
            } catch (Exception e) {
                log.error("Failed to cancel payment for booking {}: {}", bookingId, e.getMessage());
            }
        }

        // Find successful payment → refund
        var successPayment = paymentRepository.findSuccessfulByBookingId(bookingId);
        if (successPayment.isPresent()) {
            Payment payment = successPayment.get();

            // Get refund amount from event (or full amount)
            BigDecimal refundAmount = payment.getAmount();
            if (node.has("refundAmount") && !node.get("refundAmount").isNull()) {
                refundAmount = new BigDecimal(node.get("refundAmount").asText());
            }

            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("No refund required for booking {}: amount={}", bookingId, refundAmount);
                processedEventRepository.save(new ProcessedEvent(EVENT_TYPE, eventId));
                return;
            }

            UUID requestedBy = payment.getUserId();
            if (node.has("userId") && !node.get("userId").isNull()) {
                requestedBy = UUID.fromString(node.get("userId").asText());
            }

            try {
                initRefundUseCase.execute(
                        payment.getId(), refundAmount,
                        "Booking cancelled", requestedBy,
                        "booking-cancel-refund:" + bookingId
                );
                log.info("Refund initiated for booking {}: amount={}", bookingId, refundAmount);
            } catch (Exception e) {
                log.error("Failed to refund for booking {}: {}", bookingId, e.getMessage());
            }
        }

        // Mark processed
        processedEventRepository.save(new ProcessedEvent(EVENT_TYPE, eventId));
    }
}
