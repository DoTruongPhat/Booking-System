package com.booking.camunda.application.port.in;

import com.booking.camunda.domain.event.HotelCreatedEvent;
import com.booking.camunda.domain.model.HotelApprovalContext;
import com.booking.camunda.domain.model.WorkflowInstance;

public interface StartHotelApprovalWorkflowUseCase {
    WorkflowInstance start(HotelApprovalContext context);

    WorkflowInstance startFromEvent(HotelCreatedEvent event);
}
