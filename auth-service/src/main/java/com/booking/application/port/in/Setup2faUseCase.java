package com.booking.application.port.in;

import com.booking.presentation.response.TwoFactorResponse;

/**
 * Use Case: Setup 2FA
 * → Tạo QR code, secret key cho user
 */
public interface Setup2faUseCase {
    TwoFactorResponse setup2fa(String username);
}
