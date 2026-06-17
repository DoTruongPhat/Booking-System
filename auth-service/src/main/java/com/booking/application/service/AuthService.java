package com.booking.application.service;

import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.RegisterResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request,
                        String ipAddress, String userAgent);


    RegisterResponse register(RegisterRequest request);
}
    