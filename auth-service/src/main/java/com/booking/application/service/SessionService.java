package com.booking.application.service;

import java.util.UUID;

/**
 * SessionService = xử lý các thao tác liên quan đến session/token
 *
 * Tại sao tạo service riêng thay vì dùng AuthService?
 * → Single Responsibility Principle
 * → AuthService = xử lý login/logout/register
 * → SessionService = xử lý quản lý session (revoke, kick...)
 * → Tách ra → dễ maintain, dễ test
 */
public interface SessionService {
    /**
     * Revoke tất cả token của 1 user
     * Dùng khi: admin kick user, user đổi password
     *
     * @param userId ID của user cần revoke
     */
    void revokeAllSessions(UUID userId);

    /**
     * Revoke 1 token cụ thể theo JTI
     * Dùng khi: admin kick 1 session cụ thể
     *
     * @param jti JWT ID của token cần revoke
     */
    void revokeSession(String jti);
}
