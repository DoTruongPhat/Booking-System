package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordRequest {
    @Size(min = 3, max = 100)
    private String username;        // optional — đổi username

    @NotBlank
    @Size(min = 8)
    private String newPassword;
}