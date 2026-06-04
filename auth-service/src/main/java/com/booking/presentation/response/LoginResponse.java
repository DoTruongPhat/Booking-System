package com.booking.presentation.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String username;
    private String email;
    private List<String> roles;
    private String timezone;
    private boolean twoFactorRequired;
    private String mfaSessionToken;
}
