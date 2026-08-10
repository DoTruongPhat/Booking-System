package com.booking.camunda.application.port.in;

public interface ManageWorkflowTaskUseCase {
    void claimTask(String taskId, String userId);

    void unclaimTask(String taskId);
}
