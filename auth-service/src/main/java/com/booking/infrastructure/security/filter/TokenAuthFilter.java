package com.booking.infrastructure.security.filter;

import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.domain.exception.ErrorCode;
import com.booking.infrastructure.external.cache.TokenCacheService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TokenAuthFilter (Phase A.4 — Stateless)
 *
 * Verify ACCESS token mỗi request:
 *  1. Verify JWT signature + exp
 *  2. Check blacklist (Redis fast-path → DB fallback)
 *  3. Build authorities từ JWT claims (roles + permissions)
 *  4. Set SecurityContext
 *
 * KHÔNG còn:
 *  - SessionService.verifyActive(jti)  — đã bỏ user_sessions table
 *  - findByTokenHash(jwt)              — access không lưu DB
 *  - tokenCacheService.getToken(...)   — không cache raw access nữa
 */
@Component
@RequiredArgsConstructor
@Log4j2
@Order(2)
public class TokenAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenCacheService tokenCacheService;
    private final TokenBlacklistRepositoryPort blacklistRepositoryPort;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = extractToken(request);

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Verify JWT signature + exp
            if (!jwtService.validateToken(jwt)) {
                log.warn("[Token] Invalid JWT from IP: {}", request.getRemoteAddr());
                sendUnauthorized(response);
                return;
            }

            // 2. Extract claims
            String username = jwtService.extractUsername(jwt);
            String jti = jwtService.extractJti(jwt);

            // 3. Check blacklist — Redis fast-path
            if (tokenCacheService.isBlacklisted(jti)) {
                log.warn("[Token] Blacklisted (cache), jti: {}, user: {}", jti, username);
                sendUnauthorized(response);
                return;
            }

            // 4. Fallback DB blacklist (Redis có thể miss sau restart)
            if (blacklistRepositoryPort.isBlacklisted(jti)) {
                log.warn("[Token] Blacklisted (DB), jti: {}, user: {}", jti, username);
                // Repopulate cache cho lần sau (optional - bỏ qua expiresAt vì không biết chính xác)
                sendUnauthorized(response);
                return;
            }

            // 5. Build authorities từ JWT claims
            List<GrantedAuthority> authorities = new ArrayList<>();

            List<String> roles = jwtService.extractRoles(jwt);
            roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                    .forEach(authorities::add);
            roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.trim()))
                    .forEach(authorities::add);

            List<String> permissions = jwtService.extractPermissions(jwt);
            permissions.stream()
                    .map(permission -> new SimpleGrantedAuthority(permission.trim()))
                    .forEach(authorities::add);

            log.debug("[Token] User: {} authorities: {}", username, authorities);

            // 6. Set SecurityContext
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.debug("[Token] Authenticated: {}", username);

            filterChain.doFilter(request, response);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false," +
                        "\"errorCode\":\"" + ErrorCode.AUTH_003 + "\"," +
                        "\"message\":\"" + ErrorCode.AUTH_003_MSG + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/exchange")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/refresh")
                || path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.startsWith("/api/internal/")
                || path.startsWith("/internal/")
                || path.equals("/actuator/health");
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
