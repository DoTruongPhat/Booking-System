package com.booking.camunda.presentation.response;

import com.booking.camunda.domain.model.WorkflowInstance;
import lombok.Builder;

@Builder
public record ProcessInstanceResponse(
        String processInstanceId,
        String processDefinitionId,
        String businessKey,
        boolean ended
) {
    public static ProcessInstanceResponse from(WorkflowInstance instance) {
        return ProcessInstanceResponse.builder()
                .processInstanceId(instance.processInstanceId())
                .processDefinitionId(instance.processDefinitionId())
                .businessKey(instance.businessKey())
                .ended(instance.ended())
                .build();
    }
}
