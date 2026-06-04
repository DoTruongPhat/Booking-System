package com.booking.application.port.in;


import com.booking.presentation.request.ForgotPasswordRequest;

/**
 * Use Case: Quên mật khẩu
 * → Gửi email reset password
 */
public interface ForgotPasswordUseCase {
    void forgotPassword(ForgotPasswordRequest request);
}
