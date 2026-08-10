package com.booking.camunda.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotelApprovalDecisionRequest {

    @NotBlank(message = "decision is required")
    private String decision;

    private String comment;
}
