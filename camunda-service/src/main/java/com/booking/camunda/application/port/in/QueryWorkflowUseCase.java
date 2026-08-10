package com.booking.camunda.application.port.in;

import com.booking.camunda.domain.model.WorkflowInstance;
import com.booking.camunda.domain.model.WorkflowTask;

import java.util.List;

public interface QueryWorkflowUseCase {
    WorkflowInstance getProcessInstance(String processInstanceId);

    WorkflowInstance getProcessByBusinessKey(String businessKey);

    List<WorkflowTask> getTasks(String candidateGroup, String assignee);

    WorkflowTask getTask(String taskId);
}
