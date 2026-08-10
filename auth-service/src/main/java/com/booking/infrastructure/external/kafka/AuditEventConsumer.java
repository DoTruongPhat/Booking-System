package com.booking.infrastructure.external.kafka;

import com.booking.application.service.AuditLogCommand;
import com.booking.application.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Log4j2
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener(
            topics = "${app.kafka.topic.audit-events:audit-events}",
            containerFactory = "auditKafkaListenerContainerFactory")
    public void consume(Map<String, Object> event) {
        try {
            auditLogService.record(toCommand(event));
        } catch (Exception ex) {
            log.warn("Failed to persist audit event: {}", ex.getMessage(), ex);
        }
    }

    private AuditLogCommand toCommand(Map<String, Object> event) {
        return new AuditLogCommand(
                text(event, "eventKey"),
                text(event, "eventType", "action", "type"),
                text(event, "action", "eventType", "type"),
                text(event, "source"),
                uuid(event, "actorId"),
                text(event, "actorExternalId"),
                text(event, "actorName"),
                text(event, "actorRole"),
                text(event, "entityType", "resource"),
                text(event, "entityId", "resourceId"),
                uuid(event, "userId"),
                text(event, "resource", "entityType"),
                uuid(event, "resourceId"),
                text(event, "description", "message"),
                text(event, "ipAddress"),
                text(event, "userAgent"),
                text(event, "traceId"),
                metadata(event),
                instant(event, "createdAt", "occurredAt", "timestamp")
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metadata(Map<String, Object> event) {
        Object metadata = event.get("metadata");
        return metadata instanceof Map<?, ?> map ? (Map<String, Object>) map : event;
    }

    private String text(Map<String, Object> event, String... keys) {
        for (String key : keys) {
            Object value = event.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private UUID uuid(Map<String, Object> event, String key) {
        String value = text(event, key);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Instant instant(Map<String, Object> event, String... keys) {
        String value = text(event, keys);
        if (value == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return Instant.now();
        }
    }
}
