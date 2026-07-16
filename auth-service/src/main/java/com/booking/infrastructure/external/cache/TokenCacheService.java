package com.booking.infrastructure.external.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * TokenCacheService = Redis cache cho auth flows
 *
 * - KHÔNG còn cache raw access token (Cách 1 Stateless)
 * - Reset password OTP: vẫn cache
 * - MFA session: vẫn cache (2FA flow)
 * - Blacklist: cache song song với DB (DB là source of truth, Redis là fast path)
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class TokenCacheService {

    private final StringRedisTemplate redisTemplate;

    // ============ PREFIXES ============
    private static final String RESET_PREFIX     = "reset:";
    private static final String MFA_PREFIX       = "mfa:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // ============ TTL mặc định ============
    private static final long RESET_TTL_MINUTES = 15;
    private static final long MFA_TTL_MINUTES   = 5;

    // ============ RESET PASSWORD ============

    public void saveResetToken(String token, String userId) {
        redisTemplate.opsForValue().set(
                RESET_PREFIX + token,
                userId,
                RESET_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public String getResetToken(String token) {
        return redisTemplate.opsForValue().get(RESET_PREFIX + token);
    }

    public void deleteResetToken(String token) {
        redisTemplate.delete(RESET_PREFIX + token);
    }

    // ============ MFA SESSION ============

    /**
     * Lưu MFA session token (TTL 5 phút)
     * Key: "mfa:{sessionToken}" → value: userId
     * Dùng trong flow 2FA: login xong → chờ verify OTP
     */
    public void saveMfaSession(String sessionToken, String userId) {
        redisTemplate.opsForValue().set(
                MFA_PREFIX + sessionToken,
                userId,
                MFA_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    public String getMfaSession(String sessionToken) {
        return redisTemplate.opsForValue().get(MFA_PREFIX + sessionToken);
    }

    public void deleteMfaSession(String sessionToken) {
        redisTemplate.delete(MFA_PREFIX + sessionToken);
    }

    // ============ BLACKLIST ============

    /**
     * Blacklist jti với TTL = thời gian còn lại của token gốc
     * → Sau khi hết TTL, Redis tự xoá → đỡ phải cleanup
     *
     * Gọi khi: logout, admin revoke, new login.
     *
     * @param jti        JWT ID
     * @param expiresAt  thời điểm token gốc hết hạn
     */
    public void blacklist(String jti, ZonedDateTime expiresAt) {
        long secondsToLive = Duration.between(ZonedDateTime.now(), expiresAt).getSeconds();
        if (secondsToLive <= 0) {
            // Token đã hết hạn → không cần blacklist
            log.debug("[Cache] Skip blacklist for already-expired jti={}", jti);
            return;
        }
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "1",
                secondsToLive,
                TimeUnit.SECONDS
        );
    }

    /**
     * Overload: blacklist với TTL tính bằng giờ (backward-compat)
     * → Khuyến nghị dùng overload nhận ZonedDateTime cho rõ ràng
     */
    public void blacklistToken(String jti, long ttlHours) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "1",
                ttlHours,
                TimeUnit.HOURS
        );
    }

    /**
     * Check Redis cache cho jti blacklist (KHÔNG fallback DB)
     * → TokenAuthFilter gọi đầu tiên cho fast path
     * → Nếu miss, fallback gọi TokenBlacklistRepositoryPort.isBlacklisted(jti)
     *
     * @return true nếu Redis có entry blacklist:{jti}
     */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }

    /**
     * Xoá entry blacklist khỏi cache (hiếm khi cần)
     */
    public void removeFromBlacklist(String jti) {
        redisTemplate.delete(BLACKLIST_PREFIX + jti);
    }
}