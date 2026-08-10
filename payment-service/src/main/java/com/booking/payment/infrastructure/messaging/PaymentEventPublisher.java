package com.booking.payment.infrastructure.messaging;

import com.booking.payment.application.port.out.PaymentEventPublisherPort;
import com.booking.payment.domain.event.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher implements PaymentEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.payment-events:payment-events}")
    private String topic;

    @Override
    public void publish(PaymentEvent event) {
        try {
            String key = extractKey(event);
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS);
            log.info("Published {}: key={}", event.getClass().getSimpleName(), key);
        } catch (Exception e) {
            log.error("Failed to publish payment event: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to publish payment event", e);
        }
    }

    private String extractKey(PaymentEvent event) {
        return switch (event) {
            case PaymentEvent.PaymentInitiated e -> e.bookingId().toString();
            case PaymentEvent.PaymentSucceeded e -> e.bookingId().toString();
            case PaymentEvent.PaymentFailed e -> e.bookingId().toString();
            case PaymentEvent.PaymentExpired e -> e.bookingId().toString();
            case PaymentEvent.PaymentCancelled e -> e.bookingId().toString();
            case PaymentEvent.RefundCompleted e -> e.bookingId().toString();
            case PaymentEvent.RefundFailed e -> e.bookingId().toString();
        };
    }
}
