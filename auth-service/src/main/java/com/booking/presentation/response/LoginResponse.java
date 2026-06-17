package com.booking.presentation.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String username;
    private String email;
    private List<String> roles;
    @JsonIgnore
    private String token;
    private String timezone;
    private boolean twoFactorRequired;
    private String mfaSessionToken;
}
