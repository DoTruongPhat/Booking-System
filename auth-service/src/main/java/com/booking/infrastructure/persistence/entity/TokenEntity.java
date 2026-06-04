package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "tokens", schema = "auth",
        indexes = {
                @Index(name = "idx_tokens_user_active", columnList = "user_id, is_active"),
                @Index(name = "idx_tokens_hash_active", columnList = "token_hash, is_active")
        })
public class TokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "token_hash", nullable = false, length = 512, unique = true)
    private String tokenHash;

    @Column(name = "token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String tokenEncrypted;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private ZonedDateTime lastUsedAt = ZonedDateTime.now();

    @Column(name = "deactivated_at")
    private ZonedDateTime deactivatedAt;

    @Column(name = "deactivation_reason", length = 100)
    private String deactivationReason;

    @Column(name = "jti", nullable = false, length = 255)
    private String jti;

    //Business logic methods

    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivatedAt = ZonedDateTime.now();
        this.deactivationReason = reason;
    }

    public void updateLastUsed() {
        this.lastUsedAt = ZonedDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────


    public UUID getId() { return id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

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

    public ZonedDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(ZonedDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public ZonedDateTime getDeactivatedAt() { return deactivatedAt; }
    public String getDeactivationReason() { return deactivationReason; }

    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }
}
