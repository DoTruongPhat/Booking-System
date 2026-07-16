package com.booking.gateway.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class IdempotencyRedisService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyRedisService.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final IdempotencyProperties props;
    private final ObjectMapper objectMapper;

    public IdempotencyRedisService(
            ReactiveStringRedisTemplate redisTemplate,
            IdempotencyProperties props,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    // ──────────────────────────────────────────────
    // Lock operations
    // ──────────────────────────────────────────────

    /**
     * Thử acquire lock bằng SETNX.
     *
     * @return true nếu lock được (lần đầu), false nếu key đã tồn tại
     */
    public Mono<Boolean> acquireLock(String idempotencyKey) {
        String lockKey = props.buildLockKey(idempotencyKey);
        Duration ttl = props.getLockTtl();

        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", ttl)
                .doOnNext(acquired -> {
                    if (!acquired) {
                        log.debug("Lock already held for key: {}", lockKey);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Redis lock error for key {}: {}", lockKey, e.getMessage());
                    // Fail-open: nếu Redis lỗi, cho phép request đi qua (tránh block toàn bộ)
                    return Mono.just(true);
                });
    }

    /**
     * Xóa lock sau khi đã cache response.
     * Không cần thiết về mặt functional (lock tự expire),
     * nhưng giúp giải phóng sớm để retry nhanh hơn nếu cần.
     */
    public Mono<Boolean> releaseLock(String idempotencyKey) {
        String lockKey = props.buildLockKey(idempotencyKey);
        return redisTemplate.opsForValue()
                .delete(lockKey)
                .onErrorResume(e -> {
                    log.warn("Failed to release lock for key {}: {}", lockKey, e.getMessage());
                    return Mono.just(false);
                });
    }

    // ──────────────────────────────────────────────
    // Cache operations
    // ──────────────────────────────────────────────

    /**
     * Lưu cached response vào Redis.
     * Chỉ gọi sau khi đã nhận response thành công (2xx) từ downstream.
     */
    public Mono<Void> cacheResponse(String userId, String idempotencyKey, CachedResponse response) {
        String cacheKey = props.buildCacheKey(userId, idempotencyKey);

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(response))
                .flatMap(json -> redisTemplate.opsForValue()
                        .set(cacheKey, json, props.getCacheTtl()))
                .doOnSuccess(ok -> log.debug("Cached response for key: {}", cacheKey))
                .onErrorResume(e -> {
                    log.error("Failed to cache response for key {}: {}", cacheKey, e.getMessage());
                    return Mono.just(false);
                })
                .then();
    }

    /**
     * Lấy cached response từ Redis.
     *
     * @return Mono.empty() nếu không có cache
     */
    public Mono<CachedResponse> getCachedResponse(String userId, String idempotencyKey) {
        String cacheKey = props.buildCacheKey(userId, idempotencyKey);

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .flatMap(json -> {
                    try {
                        CachedResponse cached = objectMapper.readValue(json, CachedResponse.class);
                        log.debug("Cache hit for key: {}", cacheKey);
                        return Mono.just(cached);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize cached response for key {}: {}", cacheKey, e.getMessage());
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Redis get error for key {}: {}", cacheKey, e.getMessage());
                    return Mono.empty();
                });
    }
}