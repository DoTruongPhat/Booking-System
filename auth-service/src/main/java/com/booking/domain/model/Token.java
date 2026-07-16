package com.booking.domain.model;

import com.booking.domain.enums.DeactivationReason;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Token domain model - Pure Java
 *
 * Đại diện cho 1 REFRESH TOKEN đã được issue.
 * Access token KHÔNG lưu vào DB (stateless).
 *
 * Business logic:
 * - deactivate(): tắt token (logout, new login, admin revoke)
 * - isExpired(): check TTL
 */
public class Token {

    private UUID id;
    private User user;
    private String tokenHash;       // SHA-256 của REFRESH token
    private String jti;             // JWT ID của refresh token (UNIQUE)
    private boolean isActive = true;
    private ZonedDateTime expiresAt;    // ← MỚI: TTL của refresh (7 ngày)
    private String ipAddress;
    private String userAgent;
    private ZonedDateTime createdAt;
    private ZonedDateTime deactivatedAt;
    private String deactivationReason;

    public Token() {}

    // ── Business Methods ──────────────────────────────────────

    /**
     * Vô hiệu hóa token
     * → Gọi khi logout, login mới, admin revoke
     *
     * @param reason mã lý do, nên dùng DeactivationReason enum
     *               (giữ kiểu String để mapper JPA lưu thẳng vào column)
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivatedAt = ZonedDateTime.now();
        this.deactivationReason = reason;
    }

    /**
     * Overload: nhận enum cho code clean hơn ở service layer
     */
    public void deactivate(DeactivationReason reason) {
        deactivate(reason.name());
    }

    /**
     * Check token đã hết hạn chưa
     * → Cleanup job + verify flow dùng method này
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(ZonedDateTime.now());
    }

    // ── Getters & Setters ─────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public ZonedDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(ZonedDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }

    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String reason) { this.deactivationReason = reason; }
}