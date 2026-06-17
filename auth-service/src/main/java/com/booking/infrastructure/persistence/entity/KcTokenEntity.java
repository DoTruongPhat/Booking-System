package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "kc_tokens", schema = "auth")
public class KcTokenEntity {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "kc_user_id", nullable = false, length = 100)
    private String kcUserId;

    @Column(name = "kc_access_token", nullable = false, columnDefinition = "TEXT")
    private String kcAccessToken;

    @Column(name = "kc_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String kcRefreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private ZonedDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token_expires_at")
    private ZonedDateTime refreshTokenExpiresAt;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private ZonedDateTime issuedAt;

    @Column(name = "last_refreshed_at")
    private ZonedDateTime lastRefreshedAt;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getKcUserId() { return kcUserId; }
    public void setKcUserId(String kcUserId) { this.kcUserId = kcUserId; }

    public String getKcAccessToken() { return kcAccessToken; }
    public void setKcAccessToken(String kcAccessToken) { this.kcAccessToken = kcAccessToken; }

    public String getKcRefreshToken() { return kcRefreshToken; }
    public void setKcRefreshToken(String kcRefreshToken) { this.kcRefreshToken = kcRefreshToken; }

    public ZonedDateTime getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public void setAccessTokenExpiresAt(ZonedDateTime t) { this.accessTokenExpiresAt = t; }

    public ZonedDateTime getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public void setRefreshTokenExpiresAt(ZonedDateTime t) { this.refreshTokenExpiresAt = t; }

    public ZonedDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(ZonedDateTime issuedAt) { this.issuedAt = issuedAt; }

    public ZonedDateTime getLastRefreshedAt() { return lastRefreshedAt; }
    public void setLastRefreshedAt(ZonedDateTime lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }
}
