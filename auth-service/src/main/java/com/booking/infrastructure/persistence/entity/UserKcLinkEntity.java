package com.booking.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserKcLinkEntity - JPA entity cho bảng auth.user_kc_links
 * 1:1 với UserEntity
 */
@Entity
@Table(name = "user_kc_links", schema = "auth")
public class UserKcLinkEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "kc_user_id", nullable = false, length = 100)
    private String kcUserId;

    @Column(name = "kc_provider", length = 50)
    private String kcProvider;

    @Column(name = "auth_source", nullable = false, length = 20)
    private String authSource = "KEYCLOAK";

    @Column(name = "kc_synced_at")
    private ZonedDateTime kcSyncedAt;

    @Column(name = "sync_status", length = 20)
    private String syncStatus = "SYNCED";

    @Column(name = "sync_version")
    private long syncVersion = 1L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    public UserKcLinkEntity() {}

    // ── Getters & Setters ─────────────────────────────────────

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getKcUserId() { return kcUserId; }
    public void setKcUserId(String kcUserId) { this.kcUserId = kcUserId; }

    public String getKcProvider() { return kcProvider; }
    public void setKcProvider(String kcProvider) { this.kcProvider = kcProvider; }

    public String getAuthSource() { return authSource; }
    public void setAuthSource(String authSource) { this.authSource = authSource; }

    public ZonedDateTime getKcSyncedAt() { return kcSyncedAt; }
    public void setKcSyncedAt(ZonedDateTime kcSyncedAt) { this.kcSyncedAt = kcSyncedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public long getSyncVersion() { return syncVersion; }
    public void setSyncVersion(long syncVersion) { this.syncVersion = syncVersion; }

    public ZonedDateTime getCreatedAt() { return createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    public UserEntity getUser() { return user; }
}