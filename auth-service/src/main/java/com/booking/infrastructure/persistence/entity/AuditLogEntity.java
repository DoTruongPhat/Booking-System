package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs", schema = "auth")
public class AuditLogEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "event_key", length = 120, unique = true)
    private String eventKey;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "source", length = 30)
    private String source;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_external_id", length = 120)
    private String actorExternalId;

    @Column(name = "actor_name", length = 160)
    private String actorName;

    @Column(name = "actor_role", length = 60)
    private String actorRole;

    @Column(name = "entity_type", length = 80)
    private String entityType;

    @Column(name = "entity_id", length = 120)
    private String entityId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource", length = 100)
    private String resource;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "trace_id", length = 120)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    public Instant getCreatedAtInstant() {
        return createdAt == null ? null : createdAt.toInstant();
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (eventType == null || eventType.isBlank()) {
            eventType = action;
        }
        if (source == null || source.isBlank()) {
            source = "SYSTEM";
        }
    }
}
