package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

/**
 * Request Keycloak gửi đến Auth Service
 * → verify username/password
 * → Keycloak không connect DB trực tiếp
 */
@Data
@Builder
public class VerifyUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

}
