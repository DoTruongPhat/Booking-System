package com.booking.infrastructure.external.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Ngăn chặn xử lý nhiều lần 1 request giống nhau
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class IdempotencyCacheService {
    private final StringRedisTemplate stringRedisTemplate;

    private static final long TTL_HOURS = 24;
    private static final String PREFIX = "idempotency";

    /**
     * Lưu response vào Redis với TTL 24 giờ
     */
    public void save(String key, String response) {
        stringRedisTemplate.opsForValue().set(
                PREFIX + key,
                response,
                TTL_HOURS,
                TimeUnit.HOURS);
    }

    /**
     * Lấy response đã cache
     * Null nếu chưa có hoặc đã hết TTL
     */
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(PREFIX + key);
    }

}
