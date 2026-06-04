package com.booking.infrastructure.external.kafka;

import com.booking.application.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class EmailProducer {
    private final KafkaTemplate<String, EmailEvent> kafkaTemplate;

    private static final String TOPIC = "email-notifications";

    /**
     * Publish email event lên Kafka topic
     * AuthService gọi method này sau khi tạo reset token
     */
    public void sendEmail(EmailEvent event) {
        log.info("[Kafka] Publishing email event: type={} to={}",
                event.getType(),
                event.getTo() != null
                        ? event.getTo().substring(0, 3) + "***"
                        : "***");

        kafkaTemplate.send(TOPIC, event);

        log.debug("[Kafka] Event published successfully");
    }
}
