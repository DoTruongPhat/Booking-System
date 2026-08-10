package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hotel_workflow_approvals", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelWorkflowApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "change_request_id")
    private UUID changeRequestId;

    @Column(name = "workflow_type", nullable = false, length = 30)
    private String workflowType;

    @Column(name = "process_instance_id", length = 100)
    private String processInstanceId;

    @Column(name = "business_key", nullable = false, length = 100, unique = true)
    private String businessKey;

    @Column(name = "current_task_id", length = 100)
    private String currentTaskId;

    @Column(name = "current_task_name")
    private String currentTaskName;

    @Column(name = "workflow_status", nullable = false, length = 40)
    @Builder.Default
    private String workflowStatus = "START_REQUESTED";

    @Column(name = "hotel_status_snapshot", nullable = false, length = 30)
    private String hotelStatusSnapshot;

    @Column(length = 100)
    private String assignee;

    @Column(length = 30)
    private String decision;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
