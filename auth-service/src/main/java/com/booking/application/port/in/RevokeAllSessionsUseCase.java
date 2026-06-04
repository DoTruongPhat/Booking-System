package com.booking.application.port.in;

import java.util.UUID;

/**
 * Use Case: Revoke tất cả sessions của user
 * → Admin kick user ra khỏi tất cả thiết bị
 */
public interface RevokeAllSessionsUseCase {
    void revokeAllSessions(UUID userId);
}
