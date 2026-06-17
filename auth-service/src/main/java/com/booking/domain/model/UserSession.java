package com.booking.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserSession - tracking 1 phiên đăng nhập
 * Single session: mỗi user chỉ có 1 session active
 * Login mới → kill session cũ
 */
public class UserSession {

    public static final String SOURCE_LOCAL = "LOCAL";
    public static final String SOURCE_KEYCLOAK = "KEYCLOAK";

    public static final String REASON_NEW_LOGIN = "NEW_LOGIN";
    public static final String REASON_LOGOUT = "LOGOUT";
    public static final String REASON_ADMIN_KILL = "ADMIN_KILL";
    public static final String REASON_TOKEN_EXPIRED = "TOKEN_EXPIRED";

    private UUID id;
    private UUID userId;
    private String sessionId;
    private String jti;              // JWT ID
    private String authSource;       // LOCAL | KEYCLOAK
    private String deviceInfo;
    private String ipAddress;
    private String userAgent;
    private ZonedDateTime issuedAt;
    private ZonedDateTime expiresAt;
    private ZonedDateTime lastActiveAt;
    private ZonedDateTime invalidatedAt;
    private String invalidationReason;

    public UserSession() {}

    /**
     * Kiểm tra session còn active không
     */
    public boolean isActive() {
        if (invalidatedAt != null) return false;
        if (expiresAt != null && ZonedDateTime.now().isAfter(expiresAt)) return false;
        return true;
    }

    /**
     * Invalidate session
     */
    public void invalidate(String reason) {
        this.invalidatedAt = ZonedDateTime.now();
        this.invalidationReason = reason;
    }

    public void touch() {
        this.lastActiveAt = ZonedDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getAuthSource() { return authSource; }
    public void setAuthSource(String authSource) { this.authSource = authSource; }

    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public ZonedDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(ZonedDateTime issuedAt) { this.issuedAt = issuedAt; }

    public ZonedDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

    public ZonedDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(ZonedDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public ZonedDateTime getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(ZonedDateTime invalidatedAt) { this.invalidatedAt = invalidatedAt; }

    public String getInvalidationReason() { return invalidationReason; }
    public void setInvalidationReason(String invalidationReason) { this.invalidationReason = invalidationReason; }
}
