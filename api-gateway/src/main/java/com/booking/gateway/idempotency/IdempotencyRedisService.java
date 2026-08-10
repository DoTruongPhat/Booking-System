package com.booking.gateway.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class IdempotencyRedisService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyRedisService.class);
    private static final RedisScript<Long> ACQUIRE_LOCK_SCRIPT = RedisScript.of("""
            if redis.call('exists', KEYS[1]) == 0 then
              redis.call('psetex', KEYS[1], ARGV[2], ARGV[1])
              return 1
            end
            return 0
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final IdempotencyProperties props;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public IdempotencyRedisService(
            ReactiveStringRedisTemplate redisTemplate,
            IdempotencyProperties props,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.props = props;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    public Mono<Boolean> acquireLock(String idempotencyKey, String userId) {
        String lockKey = props.buildLockKey(idempotencyKey);
        String lockValue = userId + ":" + System.currentTimeMillis();
        String ttlMillis = String.valueOf(props.getLockTtl().toMillis());

        return redisTemplate
                .execute(ACQUIRE_LOCK_SCRIPT, Collections.singletonList(lockKey), List.of(lockValue, ttlMillis))
                .next()
                .map(result -> result != null && result == 1L)
                .doOnNext(acquired -> {
                    if (!acquired) {
                        log.debug("Idempotency lock already held for key={}", lockKey);
                    }
                })
                .onErrorResume(e -> {
                    redisError("lock");
                    log.error("Redis lock error for key {}: {}", lockKey, e.getMessage());
                    return Mono.just(true);
                });
    }

    public Mono<Boolean> releaseLock(String idempotencyKey) {
        String lockKey = props.buildLockKey(idempotencyKey);
        return redisTemplate.opsForValue()
                .delete(lockKey)
                .onErrorResume(e -> {
                    redisError("release");
                    log.warn("Failed to release idempotency lock for key {}: {}", lockKey, e.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Void> cacheResponse(String userId, String idempotencyKey, CachedResponse response) {
        String cacheKey = props.buildCacheKey(userId, idempotencyKey);

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(response))
                .flatMap(json -> redisTemplate.opsForValue().set(cacheKey, json, props.getCacheTtl()))
                .doOnSuccess(ok -> log.debug("Cached idempotency response for key={}", cacheKey))
                .onErrorResume(e -> {
                    redisError("cache_write");
                    log.error("Failed to cache idempotency response for key {}: {}", cacheKey, e.getMessage());
                    return Mono.just(false);
                })
                .then();
    }

    public Mono<CachedResponse> getCachedResponse(String userId, String idempotencyKey) {
        String cacheKey = props.buildCacheKey(userId, idempotencyKey);

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .flatMap(json -> {
                    try {
                        CachedResponse cached = objectMapper.readValue(json, CachedResponse.class);
                        log.debug("Idempotency cache hit for key={}", cacheKey);
                        return Mono.just(cached);
                    } catch (JsonProcessingException e) {
                        redisError("deserialize");
                        log.error("Failed to deserialize idempotency response for key {}: {}", cacheKey, e.getMessage());
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    redisError("cache_read");
                    log.warn("Redis get error for key {}: {}", cacheKey, e.getMessage());
                    return Mono.empty();
                });
    }

    private void redisError(String operation) {
        meterRegistry.counter("idempotency.redis_error", "operation", operation).increment();
        meterRegistry.counter("idempotency_redis_error_total", "operation", operation).increment();
    }
}
