package com.booking.presentation.request;

import lombok.Data;

@Data
public class WorkflowProcessStartedRequest {
    private String businessKey;
    private String processInstanceId;
    private String workflowType;
}
