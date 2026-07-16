package com.booking.infrastructure.security.config;

import com.booking.infrastructure.security.filter.CookieToBearerFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;
    private final CookieToBearerFilter cookieToBearerFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(HttpMethod.GET, "/api/rooms/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rooms/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hotels/{id}").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Host endpoints
                        .requestMatchers(HttpMethod.GET, "/api/host/**")
                        .hasAnyRole("HOST", "ADMIN", "ADMIN_ALL")
                        .requestMatchers("/api/host/**").hasRole("HOST")

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "ADMIN_ALL")

                        // User endpoints — any authenticated user
                        .requestMatchers("/api/user/**").authenticated()

                        .anyRequest().authenticated()
                )
                // Cookie → Bearer header (before Spring reads token)
                .addFilterBefore(cookieToBearerFilter, UsernamePasswordAuthenticationFilter.class)
                // OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                );

        return http.build();
    }
}
