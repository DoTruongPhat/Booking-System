package com.booking.application.service;

/**
 * OtpService - quản lý OTP trong Redis
 *
 * Tại sao dùng Redis?
 * → Tự expire sau TTL (10 phút)
 * → Nhanh (in-memory)
 * → Tránh spam OTP bằng cách check rate limit
 */
public interface OtpService {

    /**
     * Generate OTP và lưu vào Redis
     * @param email email người nhận
     * @param purpose mục đích (FORGOT_PASSWORD, ...)
     * @return mã OTP 6 số
     */
    String generateAndStore(String email, String purpose);

    /**
     * Verify OTP
     * @return true nếu đúng và còn hạn
     */
    boolean verify(String email, String purpose, String otp);

    /**
     * Xóa OTP sau khi dùng xong
     */
    void clear(String email, String purpose);
}