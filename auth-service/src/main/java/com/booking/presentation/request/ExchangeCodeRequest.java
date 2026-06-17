package com.booking.presentation.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExchangeCodeRequest {

    @NotBlank
    private String code;              // authorization code từ KC

    @NotBlank
    private String codeVerifier;      // PKCE code_verifier

    @NotBlank
    private String redirectUri;       // Phải match với KC client config
}
