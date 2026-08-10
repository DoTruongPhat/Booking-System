package com.booking.application.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogCommand(
        String eventKey,
        String eventType,
        String action,
        String source,
        UUID actorId,
        String actorExternalId,
        String actorName,
        String actorRole,
        String entityType,
        String entityId,
        UUID userId,
        String resource,
        UUID resourceId,
        String description,
        String ipAddress,
        String userAgent,
        String traceId,
        Map<String, Object> metadata,
        Instant createdAt
) {
}
