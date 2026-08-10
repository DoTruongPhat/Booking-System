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

    /**
     * Lấy TTL từ config (mặc định 1 giờ)
     */
    private long getTtlSeconds() {
        if (appProperties.getJwt() != null && appProperties.getJwt().getTtlSeconds() > 0) {
            return appProperties.getJwt().getTtlSeconds();
        }
        return 3600L;
    }

    /**
     * Lấy refresh token TTL (mặc định 7 ngày)
     */
    private long getRefreshTtlSeconds() {
        return 7L * 24 * 60 * 60; // 7 ngày
    }

    @Override
    public String generateToken(User user) {
        return generateToken(user, UUID.randomUUID().toString());
    }

    @Override
    public String generateToken(User user, String jti) {
        // Lấy roles của user
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toList());

        // Lấy permissions (gộp từ tất cả roles, distinct)
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        Date now = Date.from(Instant.now());
        Date exp = Date.from(Instant.now().plusSeconds(getTtlSeconds()));

        // Tạo JWT - jti sẽ match với user_sessions.jti (single session tracking)
        // QUAN TRỌNG: phải có expiration claim, nếu không JJWT 0.12+ coi như invalid
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(exp)
                .id(jti)
                .signWith(getSecretKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, UUID.randomUUID().toString());
    }

    @Override
    public String generateRefreshToken(User user, String jti) {
        Date now = Date.from(Instant.now());
        Date exp = Date.from(Instant.now().plusSeconds(getRefreshTtlSeconds()));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getCode())
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("roles", roles)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(exp)
                .id(jti)
                .signWith(getSecretKey())
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.error("JWT validation failed", ex);
            return false;
        }
    }

    /**
     * Check token có phải refresh token không
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
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
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest
                    (token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException("Hash token failed", ex);
        }
    }

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
        List<String> permissions = getClaims(token)
                .get("permissions", List.class);
        return permissions != null ? permissions : List.of();
    }
}
