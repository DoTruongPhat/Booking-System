package com.booking.application.service;

import java.util.List;

/**
 * KeycloakTokenService - verify id_token từ Keycloak
 *
 * Tại sao tách thành interface?
 * → KeycloakAuthServiceImpl chỉ cần "verify id_token"
 * → Không cần biết bên trong dùng JWKS, RSA, etc.
 * → Đổi logic verify → chỉ sửa implementation
 */
public interface KeycloakTokenService {

    /**
     * Verify id_token signature + claims
     * @param idToken JWT từ Keycloak
     * @return IdTokenClaims nếu valid, throw exception nếu invalid
     */
    IdTokenClaims verifyIdToken(String idToken);

    /**
     * Thông tin user extract từ id_token
     */
    record IdTokenClaims(
        String sub,                  // KC user id
        String email,
        boolean emailVerified,
        String username,
        String firstName,
        String lastName,
        List<String> roles,
        long expiresAt
    ) {}
}
