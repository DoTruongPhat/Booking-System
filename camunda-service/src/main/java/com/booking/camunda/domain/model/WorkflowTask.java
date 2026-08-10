package com.booking.camunda.domain.model;

import java.util.Date;
import java.util.Map;

public record WorkflowTask(
        String taskId,
        String taskName,
        String taskDefinitionKey,
        String processInstanceId,
        String businessKey,
        String assignee,
        Date created,
        Map<String, Object> variables
) {
}
