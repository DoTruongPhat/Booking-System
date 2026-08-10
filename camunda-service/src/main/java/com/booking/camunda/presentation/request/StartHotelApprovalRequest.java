package com.booking.camunda.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartHotelApprovalRequest {

    @NotBlank(message = "hotelId is required")
    private String hotelId;

    @NotBlank(message = "hostId is required")
    private String hostId;

    @NotBlank(message = "hotelName is required")
    private String hotelName;

    private String city;
    private String hostEmail;
}
