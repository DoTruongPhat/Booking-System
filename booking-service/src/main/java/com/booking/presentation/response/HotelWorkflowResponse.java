package com.booking.presentation.response;

import com.booking.infrastructure.persistence.entity.HotelWorkflowApprovalEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class HotelWorkflowResponse {
    private UUID id;
    private UUID hotelId;
    private UUID changeRequestId;
    private String workflowType;
    private String processInstanceId;
    private String businessKey;
    private String currentTaskId;
    private String currentTaskName;
    private String workflowStatus;
    private String hotelStatusSnapshot;
    private String assignee;
    private String decision;
    private String rejectionReason;
    private Instant startedAt;
    private Instant completedAt;
    private Instant lastSyncedAt;
    private String lastError;
    private Instant createdAt;
    private Instant updatedAt;

    public static HotelWorkflowResponse from(HotelWorkflowApprovalEntity entity) {
        return HotelWorkflowResponse.builder()
                .id(entity.getId())
                .hotelId(entity.getHotelId())
                .changeRequestId(entity.getChangeRequestId())
                .workflowType(entity.getWorkflowType())
                .processInstanceId(entity.getProcessInstanceId())
                .businessKey(entity.getBusinessKey())
                .currentTaskId(entity.getCurrentTaskId())
                .currentTaskName(entity.getCurrentTaskName())
                .workflowStatus(entity.getWorkflowStatus())
                .hotelStatusSnapshot(entity.getHotelStatusSnapshot())
                .assignee(entity.getAssignee())
                .decision(entity.getDecision())
                .rejectionReason(entity.getRejectionReason())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .lastSyncedAt(entity.getLastSyncedAt())
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
