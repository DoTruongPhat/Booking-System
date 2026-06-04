package com.booking.application.port.in;

/**
 * Use Case: Revoke 1 session theo JTI
 * → Admin kick 1 thiết bị cụ thể
 */
public interface RevokeSessionUseCase {
    void revokeSession(String jti);
}
