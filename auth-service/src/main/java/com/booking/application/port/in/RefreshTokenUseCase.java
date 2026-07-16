package com.booking.application.port.in;

import com.booking.presentation.response.LoginResponse;

/**
 * RefreshTokenUseCase
 *
 * POST /api/auth/refresh
 *  - Verify refresh token (signature, exp, blacklist, DB)
 *  - Rotate jti — generate access + refresh mới
 *  - Deactivate row cũ trong auth.tokens
 *  - Insert row mới
 *  - Blacklist jti cũ
 *  - Trả về tokens mới
 *
 * Phase A.4 — chung jti cho access/refresh, rotate khi refresh.
 */
public interface RefreshTokenUseCase {

    /**
     * Refresh access + refresh token bằng rotate.
     *
     * @param refreshToken  raw refresh JWT từ cookie
     * @param ipAddress     IP client (audit)
     * @param userAgent     UA client (audit)
     * @return LoginResponse với token + refreshToken mới
     */
    LoginResponse refresh(String refreshToken, String ipAddress, String userAgent);
}