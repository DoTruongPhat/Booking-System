package com.booking.application.service.serviceimpl;

import com.booking.application.service.AuditLogCommand;
import com.booking.application.service.AuditLogService;
import com.booking.infrastructure.persistence.entity.AuditLogEntity;
import com.booking.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.booking.presentation.response.AuditLogResponse;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogJpaRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLogResponse record(AuditLogCommand command) {
        if (command.eventKey() != null && !command.eventKey().isBlank()) {
            var existing = auditLogRepository.findByEventKey(command.eventKey());
            if (existing.isPresent()) {
                return AuditLogResponse.from(existing.get());
            }
        }

        AuditLogEntity entity = new AuditLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setEventKey(command.eventKey());
        entity.setEventType(coalesce(command.eventType(), command.action()));
        entity.setAction(coalesce(command.action(), command.eventType(), "AUDIT_EVENT"));
        entity.setSource(coalesce(command.source(), "SYSTEM"));
        entity.setActorId(command.actorId());
        entity.setActorExternalId(command.actorExternalId());
        entity.setActorName(command.actorName());
        entity.setActorRole(command.actorRole());
        entity.setEntityType(coalesce(command.entityType(), command.resource()));
        entity.setEntityId(coalesce(command.entityId(), command.resourceId() == null ? null : command.resourceId().toString()));
        entity.setUserId(command.userId());
        entity.setResource(command.resource());
        entity.setResourceId(command.resourceId());
        entity.setDescription(command.description());
        entity.setIpAddress(command.ipAddress());
        entity.setUserAgent(command.userAgent());
        entity.setTraceId(command.traceId());
        entity.setMetadata(command.metadata());
        entity.setCreatedAt(toZonedDateTime(command.createdAt()));

        AuditLogEntity saved = auditLogRepository.save(entity);
        return AuditLogResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String eventType,
            String source,
            String actorName,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            Pageable pageable) {
        return auditLogRepository.findAll(spec(eventType, source, actorName, entityType, entityId, from, to), pageable)
                .map(AuditLogResponse::from);
    }

    private Specification<AuditLogEntity> spec(
            String eventType,
            String source,
            String actorName,
            String entityType,
            String entityId,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (hasText(eventType)) {
                predicates.add(cb.like(cb.lower(root.get("eventType")), like(eventType)));
            }
            if (hasText(source)) {
                predicates.add(cb.equal(cb.upper(root.get("source")), source.toUpperCase()));
            }
            if (hasText(actorName)) {
                predicates.add(cb.like(cb.lower(root.get("actorName")), like(actorName)));
            }
            if (hasText(entityType)) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), entityType.toUpperCase()));
            }
            if (hasText(entityId)) {
                predicates.add(cb.like(cb.lower(root.get("entityId")), like(entityId)));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), toZonedDateTime(from)));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toZonedDateTime(to)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static ZonedDateTime toZonedDateTime(Instant value) {
        return (value == null ? Instant.now() : value).atZone(ZoneId.systemDefault());
    }

    private static String like(String value) {
        return "%" + value.toLowerCase().trim() + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String coalesce(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
