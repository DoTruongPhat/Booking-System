package com.booking.infrastructure.security.config;


import com.booking.infrastructure.security.filter.ApiKeyFilter;
import com.booking.infrastructure.security.filter.IdempotencyFilter;
import com.booking.infrastructure.security.filter.TokenAuthFilter;
import com.booking.infrastructure.security.filter.TraceFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

    // Inject 2 filter đã tạo
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
        .sessionManagement(sm -> sm.
              sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Phân quyền endpoint
        .authorizeHttpRequests(auth -> auth
                // Public: không cần token
                .requestMatchers(
                        "/auth/login",
                        "/auth/exchange",            // V8: Keycloak exchange
                        "/auth/public-key",
                        "/auth/register",
                        "/auth/logout",
                        "/auth/forgot-password",
                        "/auth/2fa/verify",
                        "/auth/reset-password",
                        "/actuator/health").permitAll()
                // Internal: Keycloak gọi
                .requestMatchers("/internal/**").permitAll()
                // Tất cả còn lại: cần xác thực
                .anyRequest().authenticated()
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
}
