package com.booking.camunda.presentation.response;

import com.booking.camunda.domain.model.WorkflowTask;
import lombok.Builder;

import java.util.Date;
import java.util.Map;

@Builder
public record TaskResponse(
        String taskId,
        String taskName,
        String taskDefinitionKey,
        String processInstanceId,
        String businessKey,
        String assignee,
        Date created,
        Map<String, Object> variables
) {
    public static TaskResponse from(WorkflowTask task) {
        return TaskResponse.builder()
                .taskId(task.taskId())
                .taskName(task.taskName())
                .taskDefinitionKey(task.taskDefinitionKey())
                .processInstanceId(task.processInstanceId())
                .businessKey(task.businessKey())
                .assignee(task.assignee())
                .created(task.created())
                .variables(task.variables())
                .build();
    }
}
