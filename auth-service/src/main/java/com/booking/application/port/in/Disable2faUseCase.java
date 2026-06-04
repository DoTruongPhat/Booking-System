package com.booking.application.port.in;
/**
 * Use Case: Tắt 2FA
 * → User tắt 2FA
 */
public interface Disable2faUseCase {
    void disable2fa(String username);
}
