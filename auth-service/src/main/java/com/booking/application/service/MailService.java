package com.booking.application.service;

public interface MailService {
    /**
     * Gửi mail OTP cho user
     * @param to email người nhận
     * @param otp mã OTP 6 số
     * @param purpose mục đích (FORGOT_PASSWORD, ...)
     */
    void sendOtp(String to, String otp, String purpose);
}
