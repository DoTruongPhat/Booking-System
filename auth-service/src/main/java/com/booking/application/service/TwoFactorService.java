package com.booking.application.service;


import com.booking.presentation.request.TwoFactorRequest;
import com.booking.presentation.response.TwoFactorResponse;

public interface TwoFactorService {

    /**
     * Tạo TOTP secret và QR code cho user
     * User quét QR bằng Google Authenticator
     */
    TwoFactorResponse setup (String username);

    /**
     * Xác nhận OTP và bật 2FA
     */
    void enable(String username, String otp);

    /**
     * Tắt 2FA
     */
    void disable(String username);

    /**
     * Verify OTP khi login
     */
    boolean verifyOtp(String otp, String secret);

    /**
     * Lấy secret của user từ DB
     */
    String getSecret(String username);
}
