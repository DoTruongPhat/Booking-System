package com.booking.camunda.presentation.controller;

import com.booking.camunda.application.port.in.DecideHotelApprovalUseCase;
import com.booking.camunda.application.port.in.ManageWorkflowTaskUseCase;
import com.booking.camunda.application.port.in.QueryWorkflowUseCase;
import com.booking.camunda.application.port.in.StartHotelApprovalWorkflowUseCase;
import com.booking.camunda.domain.model.HotelApprovalContext;
import com.booking.camunda.domain.model.HotelApprovalDecision;
import com.booking.camunda.presentation.request.HotelApprovalDecisionRequest;
import com.booking.camunda.presentation.request.StartHotelApprovalRequest;
import com.booking.camunda.presentation.response.ProcessInstanceResponse;
import com.booking.camunda.presentation.response.TaskResponse;
import com.booking.camunda.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final StartHotelApprovalWorkflowUseCase startHotelApprovalWorkflowUseCase;
    private final DecideHotelApprovalUseCase decideHotelApprovalUseCase;
    private final QueryWorkflowUseCase queryWorkflowUseCase;
    private final ManageWorkflowTaskUseCase manageWorkflowTaskUseCase;

    @PostMapping("/hotel-approvals")
    public ResponseEntity<Map<String, Object>> startHotelApproval(
            @Valid @RequestBody StartHotelApprovalRequest request) {

        var result = startHotelApprovalWorkflowUseCase.start(new HotelApprovalContext(
                request.getHotelId(),
                request.getHostId(),
                request.getHotelName(),
                request.getCity(),
                request.getHostEmail()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Hotel approval workflow started",
                "data", ProcessInstanceResponse.from(result)
        ));
    }

    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<Map<String, Object>> getProcess(@PathVariable String processInstanceId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ProcessInstanceResponse.from(
                        queryWorkflowUseCase.getProcessInstance(processInstanceId))
        ));
    }

    @GetMapping("/process")
    public ResponseEntity<Map<String, Object>> getProcessByBusinessKey(@RequestParam String businessKey) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", ProcessInstanceResponse.from(
                        queryWorkflowUseCase.getProcessByBusinessKey(businessKey))
        ));
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(required = false) String candidateGroup,
            @RequestParam(required = false) String assignee) {

        var tasks = queryWorkflowUseCase.getTasks(candidateGroup, assignee)
                .stream()
                .map(TaskResponse::from)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", tasks,
                "total", tasks.size()
        ));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", TaskResponse.from(queryWorkflowUseCase.getTask(taskId))
        ));
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable String taskId) {
        String userId = SecurityUtils.getCurrentUserId().toString();
        manageWorkflowTaskUseCase.claimTask(taskId, userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Task claimed"
        ));
    }

    @PostMapping("/tasks/{taskId}/unclaim")
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable String taskId) {
        manageWorkflowTaskUseCase.unclaimTask(taskId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Task unclaimed"
        ));
    }

    @PostMapping("/hotel-approvals/tasks/{taskId}/decision")
    public ResponseEntity<Map<String, Object>> decideHotelApproval(
            @PathVariable String taskId,
            @Valid @RequestBody HotelApprovalDecisionRequest request) {

        String reviewerId = SecurityUtils.getCurrentUserId().toString();
        decideHotelApprovalUseCase.decide(new HotelApprovalDecision(
                taskId,
                request.getDecision(),
                request.getComment(),
                reviewerId
        ));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hotel approval decision completed"
        ));
    }
}
