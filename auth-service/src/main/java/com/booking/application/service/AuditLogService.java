package com.booking.application.service;

import com.booking.presentation.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AuditLogService {
    AuditLogResponse record(AuditLogCommand command);

    Page<AuditLogResponse> search(
            String eventType,
            String source,
            String actorName,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            Pageable pageable);
}
