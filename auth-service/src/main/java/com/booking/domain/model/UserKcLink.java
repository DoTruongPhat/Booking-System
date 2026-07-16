package com.booking.domain.model;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * UserKcLink - Keycloak account linking info
 * Tách riêng từ User domain, 1:1 relationship.
 *
 * authSource = KEYCLOAK: chỉ KC login (password=null, cần set-password)
 * authSource = LINKED: có cả local password + KC (login cách nào cũng được)
 * authSource = LOCAL: chỉ local (KC không link)
 */
public class UserKcLink {

    private UUID userId;
    private String kcUserId;
    private String kcProvider; // google, facebook, github, null
    private String authSource; // KEYCLOAK, LINKED, LOCAL
    private ZonedDateTime kcSyncedAt;
    private String syncStatus; // SYNCED, PENDING, FAILED
    private long syncVersion;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public UserKcLink() {}

    public UserKcLink(UUID userId, String kcUserId, String kcProvider, String authSource) {
        this.userId = userId;
        this.kcUserId = kcUserId;
        this.kcProvider = kcProvider;
        this.authSource = authSource;
        this.syncStatus = "SYNCED";
        this.syncVersion = 1L;
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    // ── Business Methods ──────────────────────────────────────

    public boolean isLinked() {
        return "LINKED".equals(authSource);
    }

    public boolean isKcOnly() {
        return "KEYCLOAK".equals(authSource);
    }

    public boolean isLocalOnly() {
        return "LOCAL".equals(authSource);
    }

    /**
     * Mark user là LINKED (đã set password)
     * Sau khi user set-password thành công
     */
    public void markLinked() {
        this.authSource = "LINKED";
        this.syncStatus = "SYNCED";
        this.kcSyncedAt = ZonedDateTime.now();
        this.syncVersion++;
        this.updatedAt = ZonedDateTime.now();
    }

    public void updateSync() {
        this.kcSyncedAt = ZonedDateTime.now();
        this.syncStatus = "SYNCED";
        this.syncVersion++;
        this.updatedAt = ZonedDateTime.now();
    }

    public void markSyncFailed() {
        this.syncStatus = "FAILED";
        this.updatedAt = ZonedDateTime.now();
    }

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
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}