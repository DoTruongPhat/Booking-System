package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions", schema = "auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_sessions_session_id", columnNames = "session_id"),
                @UniqueConstraint(name = "uq_user_sessions_jti", columnNames = "jti")
        }
)
public class UserSessionEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "jti", nullable = false, length = 100)
    private String jti;

    @Column(name = "auth_source", nullable = false, length = 20)
    private String authSource = "LOCAL";

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private ZonedDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @Column(name = "last_active_at")
    private ZonedDateTime lastActiveAt;

    @Column(name = "invalidated_at")
    private ZonedDateTime invalidatedAt;

    @Column(name = "invalidation_reason", length = 50)
    private String invalidationReason;

    public UserSessionEntity() {}

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
