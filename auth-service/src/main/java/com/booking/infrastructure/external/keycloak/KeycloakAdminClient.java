package com.booking.infrastructure.external.keycloak;

import com.booking.application.port.out.KeycloakAdminPort;
import com.booking.infrastructure.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * KeycloakAdminClient - gọi Keycloak Admin API
 * Implement KeycloakAdminPort
 *
 * Dùng để: update user attributes, disable user, find user
 * Auth: Resource Owner Password (admin) → access_token → gọi Admin API
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class KeycloakAdminClient implements KeycloakAdminPort {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    private volatile String adminToken;
    private volatile long adminTokenExpiry = 0;

    @PostConstruct
    public void init() {
        log.info("[KC Admin] Initialized for realm: {}",
            appProperties.getKeycloak().getRealm());
    }

    @Override
    public void updateUserAttributes(String kcUserId, Map<String, String> attributes) {
        String url = String.format(
            "%s/admin/realms/%s/users/%s",
            appProperties.getKeycloak().getUrl(),
            appProperties.getKeycloak().getRealm(),
            kcUserId
        );

        Map<String, List<String>> kcAttributes = new HashMap<>();
        attributes.forEach((k, v) -> kcAttributes.put(k, List.of(v)));

        Map<String, Object> body = Map.of("attributes", kcAttributes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminToken());

        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
            log.info("[KC Admin] Updated attributes for user {}: {}", kcUserId, attributes.keySet());
        } catch (Exception e) {
            log.error("[KC Admin] Failed to update attributes for user {}: {}",
                kcUserId, e.getMessage());
            throw new IllegalStateException("KC Admin: update attributes failed", e);
        }
    }

    @Override
    public void disableUser(String kcUserId) {
        String url = String.format(
            "%s/admin/realms/%s/users/%s",
            appProperties.getKeycloak().getUrl(),
            appProperties.getKeycloak().getRealm(),
            kcUserId
        );

        Map<String, Object> body = Map.of("enabled", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAdminToken());

        try {
            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
            log.info("[KC Admin] Disabled user {}", kcUserId);
        } catch (Exception e) {
            log.error("[KC Admin] Failed to disable user {}: {}", kcUserId, e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public KcUserInfo getUser(String kcUserId) {
        String url = String.format(
            "%s/admin/realms/%s/users/%s",
            appProperties.getKeycloak().getUrl(),
            appProperties.getKeycloak().getRealm(),
            kcUserId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );
            return mapToUserInfo((Map<String, Object>) response.getBody());
        } catch (Exception e) {
            log.error("[KC Admin] Failed to get user {}: {}", kcUserId, e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public KcUserInfo findUserByEmail(String email) {
        String url = String.format(
            "%s/admin/realms/%s/users?email=%s&exact=true",
            appProperties.getKeycloak().getUrl(),
            appProperties.getKeycloak().getRealm(),
            email
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), List.class
            );
            List<Map<String, Object>> users = response.getBody();
            if (users != null && !users.isEmpty()) {
                return mapToUserInfo(users.get(0));
            }
            return null;
        } catch (Exception e) {
            log.error("[KC Admin] Failed to find user by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    // ── Admin token management ─────────────────────────────

    private String getAdminToken() {
        if (adminToken != null && System.currentTimeMillis() < adminTokenExpiry - 60_000) {
            return adminToken;
        }
        return refreshAdminToken();
    }

    private synchronized String refreshAdminToken() {
        String url = String.format(
            "%s/realms/%s/protocol/openid-connect/token",
            appProperties.getKeycloak().getUrl(),
            "master"  // Admin token phải lấy từ master realm
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", "admin-cli");
        body.add("username", appProperties.getKeycloak().getAdminUsername());
        body.add("password", appProperties.getKeycloak().getAdminPassword());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                url, new HttpEntity<>(body, headers), Map.class
            );

            this.adminToken = (String) response.get("access_token");
            long expiresIn = ((Number) response.get("expires_in")).longValue();
            this.adminTokenExpiry = System.currentTimeMillis() + (expiresIn * 1000);
            log.info("[KC Admin] Admin token refreshed, expires in {}s", expiresIn);
            return this.adminToken;
        } catch (Exception e) {
            log.error("[KC Admin] Failed to get admin token: {}", e.getMessage());
            throw new IllegalStateException("Cannot authenticate to Keycloak Admin", e);
        }
    }

    private KcUserInfo mapToUserInfo(Map<String, Object> kcUser) {
        if (kcUser == null) return null;
        @SuppressWarnings("unchecked")
        Map<String, List<String>> rawAttrs = (Map<String, List<String>>) kcUser.get("attributes");
        Map<String, String> attrs = new HashMap<>();
        if (rawAttrs != null) {
            rawAttrs.forEach((k, v) -> {
                if (v != null && !v.isEmpty()) attrs.put(k, v.get(0));
            });
        }
        return new KcUserInfo(
            (String) kcUser.get("id"),
            (String) kcUser.get("username"),
            (String) kcUser.get("email"),
            Boolean.TRUE.equals(kcUser.get("emailVerified")),
            (String) kcUser.get("firstName"),
            (String) kcUser.get("lastName"),
            Boolean.TRUE.equals(kcUser.get("enabled")),
            attrs
        );
    }
}
