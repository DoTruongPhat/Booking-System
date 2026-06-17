package com.booking.domain.model;

import com.booking.domain.enums.UserStatus;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User domain model - Pure Java
 * → Không có @Entity, @Table, @Column
 * → Không phụ thuộc JPA, Spring, Hibernate
 * → Chỉ chứa business logic thuần túy
 */
public class User {

    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private boolean isActive = true;
    private boolean isLocked = false;
    private int failedAttempts = 0;
    private ZonedDateTime lockedUntil;
    private String timezone;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String totpSecret;
    private boolean twoFactorEnabled = false;
    private Set<Role> roles = new HashSet<>();

    // ── Keycloak sync fields (V8) ───────────────────────────
    private String phone;
    private String kcUserId;
    private ZonedDateTime kcSyncedAt;
    /**
     * PENDING: chưa sync sang KC
     * SYNCED: đã sync
     * FAILED: sync lỗi
     * DELETED: user đã xóa ở KC
     */
    private String syncStatus = "PENDING";
    private long syncVersion = 0L;
    /**
     * LOCAL: chỉ Form A
     * KEYCLOAK: chỉ Form B
     * LINKED: có cả 2
     */
    private String authSource = "LOCAL";

    public User() {}

    // ── Business Methods ──────────────────────────────────────

    /**
     * Tăng số lần đăng nhập sai
     * → Gọi khi Keycloak verify password thất bại
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    /**
     * Reset số lần đăng nhập sai
     * → Gọi khi đăng nhập thành công
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.isLocked = false;
    }

    /**
     * Khóa tài khoản đến thời điểm cụ thể
     * → Gọi khi vượt quá số lần đăng nhập sai
     */
    public void lockUntil(ZonedDateTime until) {
        this.isLocked = true;
        this.lockedUntil = until;
    }

    /**
     * Kiểm tra tài khoản có bị khóa không
     * → Tự động mở khóa nếu đã hết thời gian khóa
     */
    public boolean isAccountLocked() {
        if (!isLocked) return false;
        if (lockedUntil != null && ZonedDateTime.now().isAfter(lockedUntil)) {
            this.isLocked = false;
            this.lockedUntil = null;
            this.failedAttempts = 0;
            return false;
        }
        return true;
    }

    /**
     * Lấy trạng thái tài khoản
     */
    public UserStatus getStatus() {
        if (!isActive) return UserStatus.INACTIVE;
        if (isAccountLocked()) return UserStatus.LOCKED;
        return UserStatus.ACTIVE;
    }

    // ── Getters & Setters (đơn giản, không validate) ──────────
    // Validation xảy ra ở Service/Use Case layer
    // Không validate ở đây vì:
    // → Khi load từ DB → không cần validate lại
    // → Mapper gọi setter → không nên throw exception

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public ZonedDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(ZonedDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getKcUserId() { return kcUserId; }
    public void setKcUserId(String kcUserId) { this.kcUserId = kcUserId; }

    public ZonedDateTime getKcSyncedAt() { return kcSyncedAt; }
    public void setKcSyncedAt(ZonedDateTime kcSyncedAt) { this.kcSyncedAt = kcSyncedAt; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public long getSyncVersion() { return syncVersion; }
    public void setSyncVersion(long syncVersion) { this.syncVersion = syncVersion; }

    public String getAuthSource() { return authSource; }
    public void setAuthSource(String authSource) { this.authSource = authSource; }
}