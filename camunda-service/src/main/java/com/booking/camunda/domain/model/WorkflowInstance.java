package com.booking.camunda.domain.model;

public record WorkflowInstance(
        String processInstanceId,
        String processDefinitionId,
        String businessKey,
        boolean ended
) {
}
