package com.booking.application.port.in;

import com.booking.presentation.request.LoginRequest;
import com.booking.presentation.request.TwoFactorRequest;
import com.booking.presentation.response.LoginResponse;

/**
 * Use Case: Verify OTP 2FA
 * → Sau khi login → verify OTP → nhận JWT
 */
public interface Verify2faUseCase {
    LoginResponse verify2fa(TwoFactorRequest request,
                            String ipAddress,
                            String userAgent);
}
