package com.booking.application.service;

import com.booking.domain.enums.HotelStatus;
import com.booking.domain.enums.HotelWorkflowStatus;
import com.booking.domain.enums.HotelWorkflowType;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.infrastructure.persistence.entity.HotelWorkflowApprovalEntity;
import com.booking.infrastructure.persistence.repository.HotelWorkflowApprovalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HotelWorkflowSyncService {

    private final HotelWorkflowApprovalJpaRepository workflowRepository;

    public void createStartRequested(
            UUID hotelId,
            UUID changeRequestId,
            HotelWorkflowType type,
            String businessKey,
            HotelStatus hotelStatus
    ) {
        workflowRepository.findByBusinessKey(businessKey).ifPresent(existing -> {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "Workflow already exists for business key " + businessKey);
        });

        workflowRepository.save(HotelWorkflowApprovalEntity.builder()
                .hotelId(hotelId)
                .changeRequestId(changeRequestId)
                .workflowType(type.name())
                .businessKey(businessKey)
                .workflowStatus(HotelWorkflowStatus.START_REQUESTED.name())
                .hotelStatusSnapshot(hotelStatus.name())
                .startedAt(Instant.now())
                .lastSyncedAt(Instant.now())
                .build());
    }

    @Transactional
    public void markProcessStarted(String businessKey, String processInstanceId, String workflowType) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setProcessInstanceId(processInstanceId);
        workflow.setWorkflowType(resolveWorkflowType(workflow, workflowType));
        workflow.setWorkflowStatus(HotelWorkflowStatus.PROCESS_STARTED.name());
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markTaskCreated(String businessKey, String taskId, String taskName) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setCurrentTaskId(taskId);
        workflow.setCurrentTaskName(taskName);
        workflow.setWorkflowStatus(HotelWorkflowStatus.WAITING_ADMIN_REVIEW.name());
        workflow.setAssignee(null);
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markTaskAssigned(String taskId, String assignee) {
        HotelWorkflowApprovalEntity workflow = workflowRepository.findByCurrentTaskId(taskId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.INVALID_REQUEST, "Workflow task not tracked: " + taskId));
        workflow.setAssignee(assignee);
        workflow.setWorkflowStatus(assignee == null || assignee.isBlank()
                ? HotelWorkflowStatus.WAITING_ADMIN_REVIEW.name()
                : HotelWorkflowStatus.CLAIMED.name());
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markDecisionStarted(String businessKey, String decision, String reviewerId, String comment) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setDecision(decision);
        workflow.setAssignee(reviewerId);
        workflow.setRejectionReason("REJECTED".equals(decision) ? comment : null);
        workflow.setWorkflowStatus("APPROVED".equals(decision)
                ? HotelWorkflowStatus.APPROVING.name()
                : HotelWorkflowStatus.REJECTING.name());
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markApproved(String businessKey, HotelStatus hotelStatus) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setDecision("APPROVED");
        workflow.setCurrentTaskId(null);
        workflow.setCurrentTaskName(null);
        workflow.setWorkflowStatus(HotelWorkflowStatus.APPROVED.name());
        workflow.setHotelStatusSnapshot(hotelStatus.name());
        workflow.setCompletedAt(Instant.now());
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markRejected(String businessKey, HotelStatus hotelStatus, String reason) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setDecision("REJECTED");
        workflow.setRejectionReason(reason);
        workflow.setCurrentTaskId(null);
        workflow.setCurrentTaskName(null);
        workflow.setWorkflowStatus(HotelWorkflowStatus.REJECTED.name());
        workflow.setHotelStatusSnapshot(hotelStatus.name());
        workflow.setCompletedAt(Instant.now());
        workflow.setLastError(null);
        workflow.setLastSyncedAt(Instant.now());
    }

    @Transactional
    public void markIncident(String businessKey, String message) {
        HotelWorkflowApprovalEntity workflow = findByBusinessKey(businessKey);
        workflow.setWorkflowStatus(HotelWorkflowStatus.INCIDENT.name());
        workflow.setLastError(message);
        workflow.setLastSyncedAt(Instant.now());
    }

    public HotelWorkflowApprovalEntity findByBusinessKey(String businessKey) {
        return workflowRepository.findByBusinessKey(businessKey)
                .orElseThrow(() -> new CoreException(CoreErrorCode.INVALID_REQUEST, "Workflow not tracked: " + businessKey));
    }

    private String resolveWorkflowType(HotelWorkflowApprovalEntity workflow, String workflowType) {
        if (workflowType == null || workflowType.isBlank()) {
            return workflow.getWorkflowType();
        }
        return workflowType;
    }
}
