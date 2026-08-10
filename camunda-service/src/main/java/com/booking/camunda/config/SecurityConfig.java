package com.booking.camunda.config;

import com.booking.camunda.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Camunda Webapp for local demo.
                        .requestMatchers("/camunda/**").permitAll()
                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Internal service-to-service
                        .requestMatchers("/internal/**").permitAll()
                        // Camunda engine REST should not be public through the gateway.
                        .requestMatchers("/engine-rest/**").hasRole("INTERNAL")
                        // Business workflow API. External callers should not know the engine name.
                        .requestMatchers("/api/workflows/hotel-approvals").hasAnyRole("INTERNAL", "ADMIN", "ADMIN_ALL")
                        .requestMatchers("/api/workflows/**").hasAnyRole("ADMIN", "ADMIN_ALL")
                        // Everything else
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
