package com.booking.application.service;

import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.RegisterResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request,
                        String ipAddress, String userAgent);

    void logout(String rawToken);

    RegisterResponse register(RegisterRequest request);

    /**
     * Gửi email reset password
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Đặt lại password mới
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Verify OTP sau khi login với 2FA
     */
    LoginResponse verify2fa(TwoFactorRequest request,
                            String ipAddress,
                            String userAgent);
}
