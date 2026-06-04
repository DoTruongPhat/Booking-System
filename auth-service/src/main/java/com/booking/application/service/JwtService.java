package com.booking.application.service;

import com.booking.domain.model.User;

import java.util.List;

public interface JwtService {

    /**
     * Tạo JWT từ thông tin user
     * Payload: username, roles, issuedAt, jti
     */
    String generateToken(User user);

    /**
     * Verify JWT signature có hợp lệ không
     */
    boolean validateToken(String token);

    /**
     * Lấy username từ JWT payload
     */
    String extractUsername(String token);

    /**
     * Lấy jti (JWT ID) từ payload
     * Dùng để revoke token
     */
    String extractJti(String token);

    /**
     * Hash JWT để lưu DB
     */
    String hashToken(String token);

    String extractUserId(String token);

    List<String> extractRoles(String token);

    /**
     * Lấy danh sách permissions từ JWT
     * Dùng trong TokenAuthFilter để set SecurityContext
     *
     * @param token raw JWT
     * @return ["ADMIN_ALL", "USER_READ", "BOOKING_CREATE"...]
     */
    List<String> extractPermissions(String token);

}
