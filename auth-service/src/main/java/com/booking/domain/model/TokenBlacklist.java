package com.booking.domain.model;

import com.booking.domain.enums.BlacklistReason;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * TokenBlacklist domain model - Pure Java
 *
 * Đại diện cho 1 jti đã bị revoke.
 * Lưu cả ACCESS jti và REFRESH jti.
 *
 * Khi token verify (TokenAuthFilter):
 *   - Check Redis blacklist:{jti} → có → 401
 *   - DB là source of truth, Redis là cache
 *
 * Cleanup job định kỳ xoá rows có expires_at < NOW()
 */
public class TokenBlacklist {

 private String jti;
 private UUID userId;
 private ZonedDateTime blacklistedAt;
 private ZonedDateTime expiresAt;
 private String reason;

 public TokenBlacklist() {}

 public TokenBlacklist(String jti,
                       UUID userId,
                       ZonedDateTime expiresAt,
                       String reason) {
  this.jti = jti;
  this.userId = userId;
  this.expiresAt = expiresAt;
  this.reason = reason;
 }

 /**
  * Overload: nhận enum cho code clean hơn
  */
 public TokenBlacklist(String jti,
                       UUID userId,
                       ZonedDateTime expiresAt,
                       BlacklistReason reason) {
  this(jti, userId, expiresAt, reason.name());
 }

 // ── Business Methods ──────────────────────────────────────

 /**
  * Check entry đã hết hạn chưa (dùng cho cleanup job)
  */
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