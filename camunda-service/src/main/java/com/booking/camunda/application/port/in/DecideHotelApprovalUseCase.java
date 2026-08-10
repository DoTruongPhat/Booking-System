package com.booking.camunda.application.port.in;

import com.booking.camunda.domain.model.HotelApprovalDecision;

public interface DecideHotelApprovalUseCase {
    void decide(HotelApprovalDecision decision);
}
