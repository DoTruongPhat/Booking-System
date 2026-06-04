package com.booking.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Token domain model - Pure Java
 * → Chứa business logic deactivate, updateLastUsed
 */
public class Token {

    private UUID id;
    private User user;
    private String tokenHash;
    private String tokenEncrypted;
    private boolean isActive = true;
    private String ipAddress;
    private String userAgent;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastUsedAt = ZonedDateTime.now();
    private ZonedDateTime deactivatedAt;
    private String deactivationReason;
    private String jti;

    public Token() {}

    // ── Business Methods ──────────────────────────────────────

    /**
     * Vô hiệu hóa token
     * → Gọi khi logout, login mới, admin revoke
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivatedAt = ZonedDateTime.now();
        this.deactivationReason = reason;
    }

    /**
     * Cập nhật thời gian dùng token gần nhất
     */
    public void updateLastUsed() {
        this.lastUsedAt = ZonedDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getTokenEncrypted() { return tokenEncrypted; }
    public void setTokenEncrypted(String tokenEncrypted) { this.tokenEncrypted = tokenEncrypted; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(ZonedDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public ZonedDateTime getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(ZonedDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }

    public String getDeactivationReason() { return deactivationReason; }
    public void setDeactivationReason(String reason) { this.deactivationReason = reason; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
}