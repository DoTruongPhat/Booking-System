package com.booking.camunda.application.service;

import com.booking.camunda.application.port.in.ManageWorkflowTaskUseCase;
import com.booking.camunda.application.port.in.QueryWorkflowUseCase;
import com.booking.camunda.application.port.out.HotelCommandPort;
import com.booking.camunda.application.port.out.WorkflowEnginePort;
import com.booking.camunda.domain.model.WorkflowInstance;
import com.booking.camunda.domain.model.WorkflowTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTaskApplicationService implements QueryWorkflowUseCase, ManageWorkflowTaskUseCase {

    private final WorkflowEnginePort workflowEnginePort;
    private final HotelCommandPort hotelCommandPort;

    @Override
    public WorkflowInstance getProcessInstance(String processInstanceId) {
        return workflowEnginePort.getProcessInstance(processInstanceId);
    }

    @Override
    public WorkflowInstance getProcessByBusinessKey(String businessKey) {
        return workflowEnginePort.getProcessByBusinessKey(businessKey);
    }

    @Override
    public List<WorkflowTask> getTasks(String candidateGroup, String assignee) {
        if (candidateGroup != null) {
            return workflowEnginePort.getTasksByCandidateGroup(candidateGroup);
        }
        if (assignee != null) {
            return workflowEnginePort.getTasksByAssignee(assignee);
        }
        return workflowEnginePort.getAllActiveTasks();
    }

    @Override
    public WorkflowTask getTask(String taskId) {
        return workflowEnginePort.getTask(taskId);
    }

    @Override
    public void claimTask(String taskId, String userId) {
        workflowEnginePort.claimTask(taskId, userId);
        hotelCommandPort.markTaskAssigned(taskId, userId);
    }

    @Override
    public void unclaimTask(String taskId) {
        workflowEnginePort.unclaimTask(taskId);
        hotelCommandPort.markTaskAssigned(taskId, null);
    }
}
