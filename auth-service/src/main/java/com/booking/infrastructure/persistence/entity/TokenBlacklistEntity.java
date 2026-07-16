package com.booking.infrastructure.persistence.entity;

import com.booking.domain.enums.BlacklistReason;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * TokenBlacklistEntity = bảng auth.tokens_blacklist
 *
 * Lưu jti bị revoke (cả ACCESS jti và REFRESH jti).
 * - User logout → blacklist 2 jti (access + refresh)
 * - Single login → blacklist refresh_jti cũ
 * - Admin revoke → blacklist toàn bộ refresh_jti của user
 *
 * Khi verify token:
 * - Check Redis blacklist:{jti} (O(1) cache hit)
 * - Nếu Redis miss → fallback check DB
 * - Nếu có → 401 ngay
 *
 * Cleanup job mỗi 24h: DELETE WHERE expires_at < NOW()
 */
@Entity
@Table(
        name = "tokens_blacklist",
        schema = "auth",
        indexes = {
                @Index(name = "idx_blacklist_user",    columnList = "user_id"),
                @Index(name = "idx_blacklist_expires", columnList = "expires_at")
        }
)
public class TokenBlacklistEntity {

 @Id
 @Column(name = "jti", nullable = false, length = 255)
 private String jti;

 @Column(name = "user_id", nullable = false)
 private UUID userId;

 @CreationTimestamp
 @Column(name = "blacklisted_at", nullable = false, updatable = false)
 private ZonedDateTime blacklistedAt;

 @Column(name = "expires_at", nullable = false)
 private ZonedDateTime expiresAt;

 @Column(name = "reason", nullable = false, length = 50)
 private String reason;

 public TokenBlacklistEntity() {}

 public TokenBlacklistEntity(String jti,
                             UUID userId,
                             ZonedDateTime expiresAt,
                             String reason) {
  this.jti = jti;
  this.userId = userId;
  this.expiresAt = expiresAt;
  this.reason = reason;
 }

 public TokenBlacklistEntity(String jti,
                             UUID userId,
                             ZonedDateTime expiresAt,
                             BlacklistReason reason) {
  this(jti, userId, expiresAt, reason.name());
 }

 // ── Business Method ───────────────────────────────────────

 public boolean isExpired() {
  return expiresAt != null && expiresAt.isBefore(ZonedDateTime.now());
 }

 // ── Getters & Setters ─────────────────────────────────────

 public String getJti() { return jti; }
 public void setJti(String jti) { this.jti = jti; }

 public UUID getUserId() { return userId; }
 public void setUserId(UUID userId) { this.userId = userId; }

 public ZonedDateTime getBlacklistedAt() { return blacklistedAt; }
 public void setBlacklistedAt(ZonedDateTime blacklistedAt) { this.blacklistedAt = blacklistedAt; }

 public ZonedDateTime getExpiresAt() { return expiresAt; }
 public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

 public String getReason() { return reason; }
 public void setReason(String reason) { this.reason = reason; }
}