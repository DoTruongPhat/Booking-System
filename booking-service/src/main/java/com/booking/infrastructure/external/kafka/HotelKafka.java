package com.booking.infrastructure.external.kafka;

import com.booking.application.port.out.HotelEventPublisherPort;
import com.booking.domain.event.CoreDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelKafka implements HotelEventPublisherPort {

    private static final String TOPIC = "core-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishHotelCreated(CoreDomainEvent.HotelCreated event) {
        publish("HotelCreated", event.hotelId().toString(), event);
    }

    @Override
    public void publishHotelApproved(CoreDomainEvent.HotelApproved event) {
        publish("HotelApproved", event.hotelId().toString(), event);
    }

    @Override
    public void publishHotelDeactivated(CoreDomainEvent.HotelDeactivated event) {
        publish("HotelDeactivated", event.hotelId().toString(), event);
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