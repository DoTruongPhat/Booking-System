package com.booking.infrastructure.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class JwtDecoderConfig {

    @Value("${app.jwt.secret-key:${JWT_SECRET_KEY:ThisIsAVeryLongSecretKeyForJWTSigning2024BookingSystem!}}")
    private String jwtSecretKey;

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(
                jwtSecretKey.getBytes(StandardCharsets.UTF_8),
                "HmacSHA384"
        );
        log.info("JWT decoder initialized with shared HS384 secret");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS384)
                .build();
    }
}
