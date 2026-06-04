package com.booking.application.port.in;

/**
 * Use Case: Bật 2FA
 * → User xác nhận OTP → enable 2FA
 */
public interface Enable2faUseCase {

    void enable2fa(String username, String otp);
}
