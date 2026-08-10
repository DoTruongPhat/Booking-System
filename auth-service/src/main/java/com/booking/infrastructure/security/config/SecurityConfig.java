package com.booking.infrastructure.security.config;

import com.booking.infrastructure.security.filter.ApiKeyFilter;
import com.booking.infrastructure.security.filter.IdempotencyFilter;
import com.booking.infrastructure.security.filter.TokenAuthFilter;
import com.booking.infrastructure.security.filter.TraceFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Log4j2
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;
    private final TokenAuthFilter tokenAuthFilter;
    private final IdempotencyFilter idempotencyFilter;
    private final TraceFilter traceFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì dùng token không phải cookie/session
                .csrf(csrf -> csrf.disable())

                // Stateless: không dùng session
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Phân quyền endpoint
                .authorizeHttpRequests(auth -> auth
                        // Public: không cần token
                        .requestMatchers(
                                "/auth/login",
                                "/auth/exchange",
                                "/auth/public-key",
                                "/auth/register",
                                "/auth/logout",
                                "/auth/forgot-password",
                                "/auth/2fa/verify",
                                "/auth/reset-password",
                                "/auth/refresh",
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/actuator/metrics",
                                "/auth/complete-profile",
                                "/auth/sso/login",
                                "/auth/sso/callback").permitAll()
                        // Internal: Keycloak gọi
                        .requestMatchers("/internal/**").permitAll()
                        // Tất cả còn lại: cần xác thực
                        .anyRequest().authenticated()
                )

                // Phân biệt 401 vs 403 cho REST standard
                .exceptionHandling(ex -> ex
                        // Anonymous user truy cập protected endpoint → 401
                        // (FE interceptor sẽ catch 401 → gọi refresh)
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("[Security] Unauthenticated access to {}: {}",
                                    request.getRequestURI(),
                                    authException.getMessage());
                            sendJsonError(
                                    response,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "AUTH_002",
                                    "Authentication required"
                            );
                        })
                        // Authenticated user nhưng thiếu role/permission → 403
                        // (FE KHÔNG refresh, hiện thông báo "không có quyền")
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("[Security] Access denied to {}: {}",
                                    request.getRequestURI(),
                                    accessDeniedException.getMessage());
                            sendJsonError(
                                    response,
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "AUTH_004",
                                    "Insufficient permissions"
                            );
                        })
                )

                // Thêm filter trước UsernamePasswordAuthenticationFilter
                // ApiKeyFilter chạy trước TokenAuthFilter
                .addFilterBefore(
                        apiKeyFilter,
                        UsernamePasswordAuthenticationFilter.class)

                .addFilterAfter(
                        tokenAuthFilter,
                        ApiKeyFilter.class)

                .addFilterAfter(idempotencyFilter,
                        TokenAuthFilter.class)

                .addFilterBefore(traceFilter,
                        ApiKeyFilter.class);

        return http.build();
    }

    /**
     * Helper: trả response JSON cho lỗi auth/access.
     */
    private void sendJsonError(HttpServletResponse response,
                               int status,
                               String errorCode,
                               String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false," +
                        "\"errorCode\":\"" + errorCode + "\"," +
                        "\"message\":\"" + message + "\"}"
        );
    }
}