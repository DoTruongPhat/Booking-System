package com.booking.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Bind config từ prefix "app" trong application.properties.
 * Thay vì @Value rải rác khắp nơi → tập trung 1 chỗ.
 *
 * Ví dụ:
 * app.security.api-key → appProperties.getSecurity().getApiKey()
 */

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String timezone;
    private KeycloakProps keycloak = new KeycloakProps();
    private SecurityProps security = new SecurityProps();
    private TokenProps token = new TokenProps();
    private JwtProps jwt = new JwtProps();

    @Data
    public static class KeycloakProps {
        private String url;
        private String realm;
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class SecurityProps {
        private String apiKey;
        private String encryptionKey;
        private String internalKey;
        private int maxFailedAttempts;
        private int lockDurationMinutes;
        private String allowedOrigins;
        private String allowedFileTypes;
    }

    @Data
    public static class TokenProps {
        private int redisTtHours;
    }

    @Data
    public static class JwtProps {
        private String secretKey;
    }

}
