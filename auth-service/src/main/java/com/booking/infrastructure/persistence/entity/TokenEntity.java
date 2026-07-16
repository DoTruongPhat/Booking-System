package com.booking.infrastructure.persistence.entity;

import com.booking.domain.enums.DeactivationReason;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * TokenEntity = bảng auth.tokens
 *
 * Đại diện cho 1 REFRESH TOKEN đã issue.
 * Access token KHÔNG lưu vào DB (stateless, chỉ check blacklist).
 *
 * V10 changes:
 *  - DROP token_encrypted (security)
 *  - DROP last_used_at (không có inactive timeout)
 *  - ADD  expires_at (refresh TTL)
 *  - jti NOT NULL (đã UNIQUE từ V3)
 */
@Entity
@Table(
        name = "tokens",
        schema = "auth",
        indexes = {
                @Index(name = "idx_tokens_user_active",     columnList = "user_id, is_active"),
                @Index(name = "idx_tokens_hash_active",     columnList = "token_hash, is_active"),
                @Index(name = "idx_tokens_jti",             columnList = "jti", unique = true),
                @Index(name = "idx_tokens_expires_active",  columnList = "expires_at")
        }
)
public class TokenEntity {

 @Id
 @GeneratedValue(strategy = GenerationType.UUID)
 @Column(name = "id", updatable = false, nullable = false)
 private UUID id;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "user_id", nullable = false)
 private UserEntity user;

 /**
  * SHA-256 của REFRESH token (64 hex chars thực tế, để 512 để safe)
  */
 @Column(name = "token_hash", nullable = false, length = 512, unique = true)
 private String tokenHash;

 /**
  * JWT ID của REFRESH token. UNIQUE + NOT NULL từ V10.
  */
 @Column(name = "jti", nullable = false, length = 255, unique = true)
 private String jti;

 @Column(name = "is_active", nullable = false)
 private boolean isActive = true;

 /**
  * Thời điểm refresh token hết hạn (7 ngày kể từ created_at).
  * Cleanup job query: WHERE expires_at < NOW()
  */
 @Column(name = "expires_at", nullable = false)
 private ZonedDateTime expiresAt;

 @Column(name = "ip_address", length = 45)
 private String ipAddress;

 @Column(name = "user_agent", length = 500)
 private String userAgent;

 @CreationTimestamp
 @Column(name = "created_at", nullable = false, updatable = false)
 private ZonedDateTime createdAt;

 @Column(name = "deactivated_at")
 private ZonedDateTime deactivatedAt;

 @Column(name = "deactivation_reason", length = 100)
 private String deactivationReason;

 // ── Business logic ──────────────────────────────────────

 /**
  * Vô hiệu hóa token (logout, new login, admin revoke)
  */
 public void deactivate(String reason) {
  this.isActive = false;
  this.deactivatedAt = ZonedDateTime.now();
  this.deactivationReason = reason;
 }

 public void deactivate(DeactivationReason reason) {
  deactivate(reason.name());
 }

 /**
  * Check token đã hết hạn chưa
  */
 public boolean isExpired() {
  return expiresAt != null && expiresAt.isBefore(ZonedDateTime.now());
 }

 // ── Getters & Setters ─────────────────────────────────────

 public UUID getId() { return id; }
 public void setId(UUID id) { this.id = id; }

 public UserEntity getUser() { return user; }
 public void setUser(UserEntity user) { this.user = user; }

 public String getTokenHash() { return tokenHash; }
 public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

 public String getJti() { return jti; }
 public void setJti(String jti) { this.jti = jti; }

 public boolean isActive() { return isActive; }
 public void setActive(boolean active) { isActive = active; }

 public ZonedDateTime getExpiresAt() { return expiresAt; }
 public void setExpiresAt(ZonedDateTime expiresAt) { this.expiresAt = expiresAt; }

 public String getIpAddress() { return ipAddress; }
 public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

 public String getUserAgent() { return userAgent; }
 public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

 public ZonedDateTime getCreatedAt() { return createdAt; }
 public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

 public ZonedDateTime getDeactivatedAt() { return deactivatedAt; }
 public void setDeactivatedAt(ZonedDateTime deactivatedAt) { this.deactivatedAt = deactivatedAt; }

 public String getDeactivationReason() { return deactivationReason; }
 public void setDeactivationReason(String reason) { this.deactivationReason = reason; }
}