package com.booking.presentation.response;

import com.booking.infrastructure.persistence.entity.AuditLogEntity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String eventType,
        String action,
        String source,
        UUID actorId,
        String actorExternalId,
        String actorName,
        String actorRole,
        String entityType,
        String entityId,
        String description,
        String ipAddress,
        String userAgent,
        String traceId,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                valueOr(entity.getEventType(), entity.getAction()),
                entity.getAction(),
                valueOr(entity.getSource(), "SYSTEM"),
                entity.getActorId(),
                entity.getActorExternalId(),
                entity.getActorName(),
                entity.getActorRole(),
                valueOr(entity.getEntityType(), entity.getResource()),
                valueOr(entity.getEntityId(), entity.getResourceId() == null ? null : entity.getResourceId().toString()),
                entity.getDescription(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getTraceId(),
                entity.getMetadata(),
                entity.getCreatedAtInstant()
        );
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
