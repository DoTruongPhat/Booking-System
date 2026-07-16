package com.booking.infrastructure.messaging;

import com.booking.application.port.out.AuditEventPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for publishing audit events.
 *
 * - Publishes to "audit-events" topic
 * - Async: does not block the main transaction
 * - Fail-open: log error but don't throw (audit should never break business logic)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditKafkaAdapter implements AuditEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topic.audit-events:audit-events}")
    private String auditTopic;

    @Override
    @Async
    public void publish(AuditEvent event) {
        try {
            // Use entityId as Kafka key for partitioning (same entity → same partition → ordered)
            String key = event.entityType() + ":" + event.entityId();

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(auditTopic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Audit] Failed to publish event {}: {}",
                                    event.eventType(), ex.getMessage());
                        } else {
                            log.debug("[Audit] Published {} for {} {} by {} (offset={})",
                                    event.eventType(),
                                    event.entityType(),
                                    event.entityId(),
                                    event.actorName(),
                                    result.getRecordMetadata().offset());
                        }
                    });

        } catch (Exception e) {
            // Fail-open: audit failure should NEVER break business logic
            log.error("[Audit] Exception publishing event {}: {}",
                    event.eventType(), e.getMessage(), e);
        }
    }
}
