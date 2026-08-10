package com.booking.presentation.controller;

import com.booking.application.service.HotelService;
import com.booking.application.service.HotelWorkflowSyncService;
import com.booking.domain.enums.HotelWorkflowType;
import com.booking.presentation.request.WorkflowDecisionSyncRequest;
import com.booking.presentation.request.WorkflowErrorRequest;
import com.booking.presentation.request.WorkflowProcessStartedRequest;
import com.booking.presentation.request.WorkflowTaskSyncRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/hotels")
@RequiredArgsConstructor
@Slf4j
public class InternalHotelWorkflowController {

    private final HotelService hotelService;
    private final HotelWorkflowSyncService workflowSyncService;

    @PostMapping("/{hotelId}/approve")
    public ResponseEntity<Map<String, Object>> approveHotel(@PathVariable UUID hotelId) {
        log.info("Internal API: Approve hotel {}", hotelId);

        hotelService.approveHotel(hotelId);
        workflowSyncService.markApproved(hotelId.toString(), com.booking.domain.enums.HotelStatus.ACTIVE);
        log.info("Hotel approved via workflow: id={}, status=ACTIVE", hotelId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hotel approved",
                "hotelId", hotelId.toString(),
                "status", "ACTIVE"
        ));
    }

    @PostMapping("/{hotelId}/reject")
    public ResponseEntity<Map<String, Object>> rejectHotel(
            @PathVariable UUID hotelId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.getOrDefault("reason", "Rejected") : "Rejected";
        log.info("Internal API: Reject hotel {}, reason={}", hotelId, reason);

        hotelService.rejectHotel(hotelId, reason);
        workflowSyncService.markRejected(hotelId.toString(), com.booking.domain.enums.HotelStatus.INACTIVE, reason);
        log.info("Hotel rejected via workflow: id={}, status=INACTIVE, reason={}",
                hotelId, reason);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hotel rejected",
                "hotelId", hotelId.toString(),
                "status", "INACTIVE",
                "reason", reason
        ));
    }

    @PostMapping("/{hotelId}/workflow-started")
    public ResponseEntity<Map<String, Object>> markWorkflowStarted(
            @PathVariable UUID hotelId,
            @RequestBody WorkflowProcessStartedRequest request) {

        workflowSyncService.markProcessStarted(
                resolveBusinessKey(request.getBusinessKey(), hotelId),
                request.getProcessInstanceId(),
                request.getWorkflowType()
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "Workflow process synced"));
    }

    @PostMapping("/{hotelId}/workflow-task-created")
    public ResponseEntity<Map<String, Object>> markWorkflowTaskCreated(
            @PathVariable UUID hotelId,
            @RequestBody WorkflowTaskSyncRequest request) {

        workflowSyncService.markTaskCreated(
                resolveBusinessKey(request.getBusinessKey(), hotelId),
                request.getTaskId(),
                request.getTaskName()
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "Workflow task synced"));
    }

    @PostMapping("/workflow-tasks/{taskId}/assignment")
    public ResponseEntity<Map<String, Object>> markWorkflowTaskAssigned(
            @PathVariable String taskId,
            @RequestBody WorkflowTaskSyncRequest request) {

        workflowSyncService.markTaskAssigned(taskId, request.getAssignee());
        return ResponseEntity.ok(Map.of("success", true, "message", "Workflow assignment synced"));
    }

    @PostMapping("/workflows/{businessKey}/decision-started")
    public ResponseEntity<Map<String, Object>> markDecisionStarted(
            @PathVariable String businessKey,
            @RequestBody WorkflowDecisionSyncRequest request) {

        workflowSyncService.markDecisionStarted(
                businessKey,
                request.getDecision(),
                request.getReviewerId(),
                request.getComment()
        );
        return ResponseEntity.ok(Map.of("success", true, "message", "Workflow decision synced"));
    }

    @PostMapping("/workflows/{businessKey}/approve")
    public ResponseEntity<Map<String, Object>> approveWorkflow(
            @PathVariable String businessKey,
            @RequestBody WorkflowDecisionSyncRequest request) {

        if (HotelWorkflowType.UPDATE_HOTEL.name().equals(request.getWorkflowType())) {
            hotelService.approveHotelChange(UUID.fromString(businessKey), request.getReviewerId(), request.getComment());
            return ResponseEntity.ok(Map.of("success", true, "message", "Hotel update approved"));
        }

        UUID hotelId = UUID.fromString(businessKey);
        hotelService.approveHotel(hotelId);
        workflowSyncService.markApproved(businessKey, com.booking.domain.enums.HotelStatus.ACTIVE);
        return ResponseEntity.ok(Map.of("success", true, "message", "Hotel approved"));
    }

    @PostMapping("/workflows/{businessKey}/reject")
    public ResponseEntity<Map<String, Object>> rejectWorkflow(
            @PathVariable String businessKey,
            @RequestBody WorkflowDecisionSyncRequest request) {

        String reason = request.getComment() != null ? request.getComment() : "Rejected";

        if (HotelWorkflowType.UPDATE_HOTEL.name().equals(request.getWorkflowType())) {
            hotelService.rejectHotelChange(UUID.fromString(businessKey), request.getReviewerId(), reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "Hotel update rejected"));
        }

        UUID hotelId = UUID.fromString(businessKey);
        hotelService.rejectHotel(hotelId, reason);
        workflowSyncService.markRejected(businessKey, com.booking.domain.enums.HotelStatus.INACTIVE, reason);
        return ResponseEntity.ok(Map.of("success", true, "message", "Hotel rejected"));
    }

    @PostMapping("/workflows/{businessKey}/incident")
    public ResponseEntity<Map<String, Object>> markWorkflowIncident(
            @PathVariable String businessKey,
            @RequestBody WorkflowErrorRequest request) {

        workflowSyncService.markIncident(businessKey, request.getMessage());
        return ResponseEntity.ok(Map.of("success", true, "message", "Workflow incident synced"));
    }

    private String resolveBusinessKey(String businessKey, UUID fallbackHotelId) {
        return businessKey != null && !businessKey.isBlank()
                ? businessKey
                : fallbackHotelId.toString();
    }
}
