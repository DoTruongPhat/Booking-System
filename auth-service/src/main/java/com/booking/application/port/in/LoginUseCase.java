package com.booking.application.port.in;

import com.booking.presentation.request.LoginRequest;
import com.booking.presentation.response.LoginResponse;

public interface LoginUseCase {
    LoginResponse login(LoginRequest request, String ipAddress, String userAgent);
}
