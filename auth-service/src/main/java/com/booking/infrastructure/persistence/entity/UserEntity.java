package com.booking.infrastructure.persistence.entity;

import com.booking.domain.enums.UserStatus;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.UserException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "auth",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uq_users_email",    columnNames = "email")
        }
)
public class UserEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 500)
    private String passwordHash;

    @JsonIgnore
    @Column(name = "password_salt", nullable = false, length = 255)
    private String passwordSalt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @JsonIgnore
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @JsonIgnore
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @JsonIgnore
    @Column(name = "locked_until")
    private ZonedDateTime lockedUntil;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "totp_secret", length = 255)
    private String totpSecret;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    // ── V8: Keycloak sync fields ───────────────────────────

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "kc_user_id", length = 100)
    private String kcUserId;

    @Column(name = "kc_synced_at")
    private ZonedDateTime kcSyncedAt;

    @Column(name = "sync_status", nullable = false, length = 20)
    private String syncStatus = "PENDING";

    @Column(name = "sync_version", nullable = false)
    private long syncVersion = 0L;

    @Column(name = "auth_source", nullable = false, length = 20)
    private String authSource = "LOCAL";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", schema = "auth",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();

    //Business Methods

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.isLocked = false;
    }

    public void lockUntil(ZonedDateTime until) {
        this.isLocked = true;
        this.lockedUntil = until;
    }

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

    public UserStatus getStatus() {
        if (!isActive) return UserStatus.INACTIVE;
        if (isAccountLocked()) return UserStatus.LOCKED;
        return UserStatus.ACTIVE;
    }

    //Getters & Setters

    public UUID getId() { return id; }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new UserException(
                    ErrorCode.USR_004,
                    ErrorCode.USR_004_MSG
            );
        if (username.length() < 3 || username.length() > 100)
            throw new UserException(
                    ErrorCode.USR_005,
                    ErrorCode.USR_005_MSG
            );
        this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (email == null || email.isBlank())
            throw new UserException(
                    ErrorCode.USR_006,
                    ErrorCode.USR_006_MSG
            );
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new UserException(
                    ErrorCode.USR_007,
                    ErrorCode.USR_007_MSG
            );
        this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank())
            throw new UserException(
                    ErrorCode.USR_008,
                    ErrorCode.USR_008_MSG
            );
        this.passwordHash = passwordHash; }

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
    public void setTimezone(String timezone) {
        if (timezone == null || timezone.isBlank())
            throw new UserException(
                    ErrorCode.USR_009,
                    ErrorCode.USR_009_MSG
            );
        this.timezone = timezone; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    public Set<RoleEntity> getRoles() { return roles; }
    public void setRoles(Set<RoleEntity> roles) { this.roles = roles; }

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

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