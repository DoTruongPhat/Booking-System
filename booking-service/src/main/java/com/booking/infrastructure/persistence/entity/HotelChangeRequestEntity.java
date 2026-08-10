package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "hotel_change_requests", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelChangeRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_changes", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> proposedChanges = new LinkedHashMap<>();

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(name = "reviewer_id", length = 100)
    private String reviewerId;

    @Column(name = "decision_comment")
    private String decisionComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
