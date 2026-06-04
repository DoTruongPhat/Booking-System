package com.booking.application.service.serviceimpl;

import com.booking.application.service.JwtService;
import com.booking.domain.model.User;
import com.booking.infrastructure.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Log4j2
public class JwtServiceImpl implements JwtService {

    private final AppProperties appProperties;

    /**
     * Tạo SecretKey từ config
     * Dùng để ký và verify JWT
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = appProperties.getJwt()
                .getSecretKey()
                .getBytes();

        return Keys.hmacShaKeyFor(keyBytes);

    }

    @Override
    public String generateToken(User user) {
        // Lấy roles của user
        // → Giữ lại để backward compatible
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toList());

        // Lấy permissions của user
        // → Đi qua tất cả roles → lấy tất cả permissions
        // → flatMap: gộp nhiều Set<Permission> thành 1 stream
        // → distinct: loại bỏ permission trùng
        //   (ADMIN và MANAGER cùng có USER_READ → chỉ lấy 1 lần)
        // → collect thành List<String> để nhúng vào JWT
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        // Tạo JWT
        return Jwts.builder()
                .setSubject(user.getUsername())     // username
                .claim("roles", roles)              // ["ADMIN", "USER"]
                .claim("permissions", permissions)  // ["ADMIN_ALL", "USER_READ"...]
                .claim("userId", user.getId())      // userId
                .issuedAt(Date.from(Instant.now())) // thời điểm tạo
                .id(UUID.randomUUID().toString())   // jti = unique ID
                .signWith(getSecretKey())
                .compact();

    }

    @Override
    public boolean validateToken(String token) {
        try{
            // Parse và verify signature
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        }catch (Exception ex){
            log.warn("[JWT] Invalid token: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return  getClaims(token).getSubject();
    }

    @Override
    public String extractUserId(String token) {
        return getClaims(token).get("userId", String.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return getClaims(token).get("roles", List.class);
    }

    @Override
    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    @Override
    public String hashToken(String token) {
        try{
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest
                    (token.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex){
            throw new RuntimeException("Hash token failed", ex);
        }
    }

    /**
     * Parse JWT và lấy Claims (payload)
     */

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        // Lấy claim "permissions" từ JWT payload
        // → List<String> vì nhúng vào dạng List lúc generateToken
        // → Trả về empty list nếu không có
        //   (tránh NullPointerException)
        List<String> permissions = getClaims(token)
                .get("permissions", List.class);
        return permissions != null ? permissions : List.of();
    }
}
