package com.booking.infrastructure.external.kafka;

import com.booking.application.port.out.BookingEventPublisherPort;
import com.booking.domain.event.CoreDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingKafka implements BookingEventPublisherPort {

    private static final String TOPIC = "core-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishBookingCreated(CoreDomainEvent.BookingCreated event) {
        publish("BookingCreated", event.bookingId().toString(), event);
    }

    @Override
    public void publishBookingConfirmed(CoreDomainEvent.BookingConfirmed event) {
        publish("BookingConfirmed", event.bookingId().toString(), event);
    }

    @Override
    public void publishBookingCancelled(CoreDomainEvent.BookingCancelled event) {
        publish("BookingCancelled", event.bookingId().toString(), event);
    }

    private void publish(String eventType, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {}: {}", eventType, ex.getMessage());
                        } else {
                            log.info("Published {}: key={}", eventType, key);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize {}: {}", eventType, e.getMessage());
        }
    }
}