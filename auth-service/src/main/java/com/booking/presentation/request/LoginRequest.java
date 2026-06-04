package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 100)
    private String password;

    private String totpCode; // Optional for 2FA

}
