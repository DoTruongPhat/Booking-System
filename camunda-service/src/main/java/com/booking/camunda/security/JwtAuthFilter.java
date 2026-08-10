package com.booking.camunda.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String[] PUBLIC_PATHS = {
            "/actuator",
            "/camunda",        // Camunda Webapp (Cockpit, Tasklist)
            "/internal"
    };

    @Value("${app.jwt.secret-key}")
    private String jwtSecret;

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip public paths
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Internal API key check
        String apiKey = request.getHeader("X-Internal-Api-Key");
        if (apiKey != null && apiKey.equals(internalApiKey)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // JWT auth
        String token = extractToken(request);
        if (token != null) {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.get("userId", String.class);
                if (userId == null || userId.isBlank()) {
                    userId = claims.getSubject();
                }
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get("roles", List.class);

                List<SimpleGrantedAuthority> authorities = roles != null
                        ? roles.stream().map(r -> new SimpleGrantedAuthority(
                                r.startsWith("ROLE_") ? r : "ROLE_" + r))
                        .toList()
                        : List.of();

                var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> "access_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
