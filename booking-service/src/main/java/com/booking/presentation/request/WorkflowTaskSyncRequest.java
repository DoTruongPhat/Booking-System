package com.booking.presentation.request;

import lombok.Data;

@Data
public class WorkflowTaskSyncRequest {
    private String businessKey;
    private String taskId;
    private String taskName;
    private String assignee;
}
