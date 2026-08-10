package com.booking.payment.infrastructure.cache;

import com.booking.payment.application.port.out.PaymentCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisPaymentCache implements PaymentCachePort {

    private static final String PREFIX = "payment:idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setIdempotencyKey(String key, String paymentId) {
        redisTemplate.opsForValue().set(PREFIX + key, paymentId, TTL);
    }

    @Override
    public Optional<String> getByIdempotencyKey(String key) {
        String value = redisTemplate.opsForValue().get(PREFIX + key);
        return Optional.ofNullable(value);
    }
}