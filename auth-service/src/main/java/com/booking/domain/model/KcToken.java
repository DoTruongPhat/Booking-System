package com.booking.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * KcToken - lưu Keycloak access_token + refresh_token
 * Để gọi KC Admin API và logout SSO
 */
public class KcToken {

    private UUID userId;
    private String kcUserId;
    private String kcAccessToken;          // encrypted at rest
    private String kcRefreshToken;        // encrypted at rest
    private ZonedDateTime accessTokenExpiresAt;
    private ZonedDateTime refreshTokenExpiresAt;
    private ZonedDateTime issuedAt;
    private ZonedDateTime lastRefreshedAt;

    public KcToken() {}

    public boolean isAccessTokenExpired() {
        return accessTokenExpiresAt != null
            && ZonedDateTime.now().isAfter(accessTokenExpiresAt);
    }

    // ── Getters & Setters ─────────────────────────────────

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
