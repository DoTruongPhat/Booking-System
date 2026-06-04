package com.booking.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * SecurityConfig = định nghĩa endpoint nào public, endpoint nào cần auth
 *
 * Tại sao dùng WebFlux (Reactive)?
 * → Spring Cloud Gateway chạy trên WebFlux (không phải Servlet)
 * → Phải dùng ServerHttpSecurity thay vì HttpSecurity
 * → @EnableWebFluxSecurity thay vì @EnableWebSecurity
 *
 * Luồng:
 * Request đến Gateway
 * → SecurityConfig check endpoint
 * → Public endpoint → cho đi thẳng
 * → Protected endpoint → check JWT (JwtAuthFilter)
 * → JWT valid → forward đến service
 * → JWT invalid → 401
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Cho phép TẤT CẢ request đi qua Gateway
                        // → Auth Service tự verify JWT
                        // → Gateway chỉ làm routing
                        .anyExchange().permitAll()
                )
                .build();
    }
}
