package com.booking.application.port.in;


import com.booking.presentation.request.ResetPasswordRequest;

/**
 * Use Case: Reset mật khẩu
 * → Dùng token từ email để đổi password
 */
public interface ResetPasswordUseCase {
    void resetPassword(ResetPasswordRequest request);
}
