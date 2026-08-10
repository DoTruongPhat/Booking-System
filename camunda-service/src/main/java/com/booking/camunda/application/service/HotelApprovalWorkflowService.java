package com.booking.camunda.application.service;

import com.booking.camunda.application.port.in.DecideHotelApprovalUseCase;
import com.booking.camunda.application.port.in.StartHotelApprovalWorkflowUseCase;
import com.booking.camunda.application.port.out.HotelCommandPort;
import com.booking.camunda.application.port.out.WorkflowEnginePort;
import com.booking.camunda.domain.event.HotelCreatedEvent;
import com.booking.camunda.domain.model.HotelApprovalContext;
import com.booking.camunda.domain.model.HotelApprovalDecision;
import com.booking.camunda.domain.model.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelApprovalWorkflowService implements
        StartHotelApprovalWorkflowUseCase,
        DecideHotelApprovalUseCase {

    private static final String HOTEL_APPROVAL_PROCESS_KEY = "hotel_approval_flow";

    private final WorkflowEnginePort workflowEnginePort;
    private final HotelCommandPort hotelCommandPort;

    @Override
    public WorkflowInstance startFromEvent(HotelCreatedEvent event) {
        return start(new HotelApprovalContext(
                event.hotelId(),
                event.ownerUserId(),
                event.name(),
                event.city(),
                event.hostEmail()
        ));
    }

    @Override
    public WorkflowInstance start(HotelApprovalContext context) {
        WorkflowInstance existing = findExisting(context.businessKey());
        if (existing != null) {
            log.info("Hotel approval workflow already exists: businessKey={}, processInstanceId={}",
                    context.businessKey(), existing.processInstanceId());
            return existing;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("hotelId", context.hotelId());
        if (context.changeRequestId() != null) {
            variables.put("changeRequestId", context.changeRequestId());
        }
        variables.put("hostId", context.hostId());
        variables.put("hotelName", context.hotelName());
        variables.put("city", context.city());
        variables.put("hostEmail", context.hostEmail());
        variables.put("workflowType", context.workflowType());
        variables.put("businessKey", context.businessKey());
        variables.put("proposedChanges", context.proposedChanges());
        variables.put("reviewStatus", "CREATED");
        variables.put("reviewReminderCycle", "R3/PT24H");

        WorkflowInstance instance = workflowEnginePort.startProcess(
                HOTEL_APPROVAL_PROCESS_KEY,
                context.businessKey(),
                variables
        );
        hotelCommandPort.markWorkflowStarted(
                context.hotelId(),
                context.businessKey(),
                instance.processInstanceId(),
                instance.processDefinitionId(),
                context.workflowType()
        );
        return instance;
    }

    @Override
    public void decide(HotelApprovalDecision request) {
        String decision = request.normalizedDecision();
        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("decision", decision);
        variables.put("comment", request.comment());
        variables.put("reviewerId", request.reviewerId());

        var task = workflowEnginePort.getTask(request.taskId());
        String workflowType = String.valueOf(task.variables().getOrDefault("workflowType", "CREATE_HOTEL"));
        String businessKey = task.businessKey() != null ? task.businessKey() : String.valueOf(task.variables().get("businessKey"));

        hotelCommandPort.markDecisionStarted(
                businessKey,
                workflowType,
                decision,
                request.reviewerId(),
                request.comment()
        );

        workflowEnginePort.completeTaskAsUser(request.taskId(), request.reviewerId(), variables);
    }

    private WorkflowInstance findExisting(String hotelId) {
        try {
            return workflowEnginePort.getProcessByBusinessKey(hotelId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
