package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;

public class UpdateSystemParamRequest {
    @NotBlank(message = "Value is required")
    private String value;

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
