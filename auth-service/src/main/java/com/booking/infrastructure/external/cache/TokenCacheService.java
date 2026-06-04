package com.booking.infrastructure.external.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenCacheService {

    private final StringRedisTemplate redisTemplate;

    // ============ PREFIXES ============
    // Mỗi loại data dùng prefix khác nhau
    // → tránh trùng key trong Redis
    // → dễ debug khi nhìn vào Redis
    private static final String TOKEN_PREFIX     = "token:";
    private static final String RESET_PREFIX     = "reset:";
    private static final String MFA_PREFIX       = "mfa:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // ============ TTL mặc định ============
    // Dùng khi chưa load được từ SystemParam
    private static final long DEFAULT_TOKEN_TTL_HOURS  = 720; // 30 ngày
    private static final long RESET_TTL_MINUTES        = 15;
    private static final long MFA_TTL_MINUTES          = 5;

    // ============ TOKEN ============

    /**
     * Lưu raw token vào Redis
     * Dùng để cache → tránh query DB mỗi request
     *
     * @param userId   làm key → "token:abc123"
     * @param rawToken JWT string → làm value
     * @param ttlHours TTL lấy từ SystemParam (TOKEN_TTL_HOURS)
     */
    public void saveToken(String userId, String rawToken, long ttlHours) {
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + userId,
                rawToken,
                ttlHours,
                TimeUnit.HOURS
        );
    }

    /**
     * Đọc token từ Redis
     * Trả về null nếu key không tồn tại hoặc đã hết TTL
     */
    public String getToken(String userId) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + userId);
    }

    /**
     * Xóa token khỏi Redis
     * Gọi khi: logout, login thiết bị mới
     */
    public void deleteToken(String userId) {
        redisTemplate.delete(TOKEN_PREFIX + userId);
    }

    /**
     * Kiểm tra token có trong Redis không
     */
    public boolean hasToken(String userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(TOKEN_PREFIX + userId));
    }

    // ============ RESET PASSWORD ============

    /**
     * Lưu reset token (TTL 15 phút)
     * Key: "reset:{token}" → value: userId
     */
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
     * Blacklist một token theo JTI
     * Gọi khi: admin kick user, user đổi password
     *
     * Key: "blacklist:{jti}" → value: "revoked"
     * TTL = thời gian còn lại của token
     *   → token hết hạn thì blacklist tự xóa
     *   → Redis không bị đầy theo thời gian
     *
     * @param jti      JWT ID lấy từ bên trong token
     * @param ttlHours thời gian còn lại của token
     */
    public void blacklistToken(String jti, long ttlHours) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + jti,
                "revoked",
                ttlHours,
                TimeUnit.HOURS
        );
    }

    /**
     * Kiểm tra JTI có bị blacklist không
     * Gọi trong TokenAuthFilter trước mỗi request
     *
     * @return true  → token bị revoke → trả 401
     *         false → token bình thường → cho đi tiếp
     */
    public boolean isBlacklisted(String jti) {
        // Boolean.TRUE.equals() → tránh NullPointerException
        // vì hasKey() có thể trả về null
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
        );
    }
}