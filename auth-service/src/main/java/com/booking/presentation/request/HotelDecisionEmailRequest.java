package com.booking.presentation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HotelDecisionEmailRequest {

    @Email
    @NotBlank
    private String to;

    @NotBlank
    private String hotelName;

    @NotBlank
    private String decision;

    private String comment;
}
