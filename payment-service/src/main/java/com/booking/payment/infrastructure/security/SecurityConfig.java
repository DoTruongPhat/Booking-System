package com.booking.payment.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: gateway callbacks
                        .requestMatchers("/api/payments/callback/**").permitAll()
                        // Public: mock gateway (dev only)
                        .requestMatchers("/mock-gateway/**").permitAll()
                        // Public: actuator health
                        .requestMatchers("/actuator/**").permitAll()
                        // Internal: service-to-service
                        .requestMatchers("/internal/**").hasRole("INTERNAL_SERVICE")
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMIN_ALL")
                        // User endpoints
                        .requestMatchers("/api/user/**").authenticated()
                        // Everything else
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
