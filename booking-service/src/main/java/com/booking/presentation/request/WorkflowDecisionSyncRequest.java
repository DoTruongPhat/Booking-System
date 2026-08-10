package com.booking.presentation.request;

import lombok.Data;

@Data
public class WorkflowDecisionSyncRequest {
    private String workflowType;
    private String decision;
    private String reviewerId;
    private String comment;
}
