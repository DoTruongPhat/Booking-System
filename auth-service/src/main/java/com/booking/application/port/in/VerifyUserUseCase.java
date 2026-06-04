package com.booking.application.port.in;

import com.booking.presentation.request.VerifyUserRequest;
import com.booking.presentation.response.VerifyUserResponse;

/**
 * VerifyUserUseCase = Use Case verify user cho Keycloak
 *
 * Tại sao cần Use Case riêng?
 * → Đây là flow riêng biệt (Remote User Federation)
 * → Keycloak gọi API này thay vì connect DB trực tiếp
 * → Tách biệt với LoginUseCase (login flow của app)
 *
 * Luồng:
 * Keycloak → POST /internal/users/verify
 *         → VerifyUserUseCase
 *         → check username/password trong DB
 *         → trả về valid: true/false + user info
 */
public interface VerifyUserUseCase {
    VerifyUserResponse verifyUser(VerifyUserRequest request);
}
