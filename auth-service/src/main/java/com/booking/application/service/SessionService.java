package com.booking.application.service;

import com.booking.domain.model.User;
import com.booking.domain.model.UserSession;

import java.util.Optional;
import java.util.UUID;

/**
 * SessionService - quản lý session tracking cho single session
 *
 * Nguyên tắc:
 * - Mỗi user chỉ có 1 session active tại 1 thời điểm
 * - Login mới → kill session cũ
 * - Verify mỗi request: check session còn active trong DB
 */
public interface SessionService {

    // ══════════════════════════════════════════════════════
    // LEGACY API (giữ để tương thích code cũ)
    // ══════════════════════════════════════════════════════

    /**
     * Revoke tất cả session của user (admin kick, đổi password,...)
     */
    void revokeAllSessions(UUID userId);

    /**
     * Revoke 1 session theo JTI
     */
    void revokeSession(String jti);

    // ══════════════════════════════════════════════════════
    // SINGLE SESSION API
    // ══════════════════════════════════════════════════════

    /**
     * Invalidate 1 session theo jti + reason
     * Dùng trong logout flow
     * @return số session bị invalidate
     */
    int invalidateByJti(String jti, String reason);

    /**
     * Kill tất cả session cũ của user (gọi trước khi tạo session mới)
     * @return số session bị kill
     */
    int killOldSessions(UUID userId, String reason);

    /**
     * Tạo session mới cho user sau khi đã kill old sessions
     */
    UserSession createSession(User user, String jti, String authSource,
                              String deviceInfo, String ipAddress,
                              String userAgent, int ttlSeconds);

    /**
     * Verify session còn active không
     * Trả về Optional.empty() nếu session invalid/expired
     */
    Optional<UserSession> verifyActive(String jti);

    /**
     * Cleanup expired sessions (gọi từ scheduled job)
     */
    int cleanupExpired();
}
