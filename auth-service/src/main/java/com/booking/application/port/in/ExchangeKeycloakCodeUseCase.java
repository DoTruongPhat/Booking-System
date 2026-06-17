package com.booking.application.port.in;

import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.request.ExchangeCodeRequest;

/**
 * Use case cho Form B (Keycloak) login
 * FE gửi code + code_verifier → BE exchange với KC → cấp JWT nội bộ
 */
public interface ExchangeKeycloakCodeUseCase {

    LoginResponse exchange(ExchangeCodeRequest request, String ipAddress, String userAgent);
}
