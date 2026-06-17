package com.booking.application.service.serviceimpl;

import com.booking.application.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Log4j2
public class OtpServiceImpl implements OtpService {

    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration ATTEMPT_TTL = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateAndStore(String email, String purpose) {
        // Bước 1: Generate 6 số ngẫu nhiên
        String otp = String.format("%06d", random.nextInt(999_999));

        // Bước 2: Lưu vào Redis
        String key = buildKey(email, purpose);
        redis.opsForValue().set(key, otp, OTP_TTL);

        // Bước 3: Reset attempt counter
        redis.delete("otp:attempts:" + email);

        log.info("[Otp] Generated OTP for {} purpose={}", email, purpose);
        return otp;
    }

    @Override
    public boolean verify(String email, String purpose, String otp) {
        // Bước 1: Check attempts (rate limit)
        String attemptsKey = "otp:attempts:" + email;
        String attemptsStr = redis.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            log.warn("[Otp] Max attempts reached for {}", email);
            return false;
        }

        // Bước 2: Get OTP từ Redis
        String key = buildKey(email, purpose);
        String storedOtp = redis.opsForValue().get(key);

        // Bước 3: Verify
        if (storedOtp != null && storedOtp.equals(otp)) {
            // Đúng → xóa OTP (dùng 1 lần)
            redis.delete(key);
            redis.delete(attemptsKey);
            log.info("[Otp] OTP verified for {}", email);
            return true;
        }

        // Sai → tăng attempts
        redis.opsForValue().increment(attemptsKey);
        redis.expire(attemptsKey, ATTEMPT_TTL);
        log.warn("[Otp] Wrong OTP for {} (attempt {})", email, attempts + 1);
        return false;
    }

    @Override
    public void clear(String email, String purpose) {
        redis.delete(buildKey(email, purpose));
    }

    private String buildKey(String email, String purpose) {
        return "otp:" + purpose + ":" + email;
    }
}