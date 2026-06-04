package com.booking.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response trả về cho Keycloak
 * → valid: true/false
 * → user info để Keycloak map user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyUserResponse {

    private boolean valid;
    private String userId;
    private String username;
    private String email;
    private List<String> roles;

}
