package com.booking.infrastructure.external.keycloak;

import com.booking.application.port.out.KeycloakTokenPort;
import com.booking.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * KeycloakTokenClient - gọi Keycloak /token endpoint
 * Implement KeycloakTokenPort
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class KeycloakTokenClient implements KeycloakTokenPort {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = createRestTemplate();

    @Override
    public TokenResponse exchangeCode(String code, String codeVerifier, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("code_verifier", codeVerifier);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", appProperties.getKeycloak().getFeClientId());

        return doExchange(body, "PKCE");
    }

    @Override
    public TokenResponse exchangeCodeConfidential(String code, String redirectUri) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        body.add("client_id", appProperties.getKeycloak().getClientId());
        body.add("client_secret", appProperties.getKeycloak().getClientSecret());

        return doExchange(body, "BFF");
    }

    @Override
    public void logoutFromKc(String refreshToken) {
        String url = String.format(
            "%s/realms/%s/protocol/openid-connect/logout",
            appProperties.getKeycloak().getUrl(),
            appProperties.getKeycloak().getRealm()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", appProperties.getKeycloak().getFeClientId());
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
            log.info("[KC] Logout successful");
        } catch (Exception e) {
            log.warn("[KC] Logout failed (refresh_token may be expired): {}",
                e.getMessage());
            // Không throw - đã invalidate session DB rồi, KC logout là best-effort
        }
    }

    // ── Helpers ───────────────────────────────────────────

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    private TokenResponse doExchange(MultiValueMap<String, String> body, String flowType) {
        String url = String.format(
                "%s/realms/%s/protocol/openid-connect/token",
                appProperties.getKeycloak().getUrl(),
                appProperties.getKeycloak().getRealm()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            log.info("[KC {}] Exchange: client_id={}", flowType, body.getFirst("client_id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    url, new HttpEntity<>(body, headers), Map.class
            );

            if (response == null) {
                throw new IllegalStateException("Empty response from Keycloak");
            }

            log.info("[KC {}] Token exchange successful", flowType);

            return new TokenResponse(
                    (String) response.get("access_token"),
                    (String) response.get("refresh_token"),
                    (String) response.get("id_token"),
                    toLong(response.get("expires_in")),
                    toLong(response.get("refresh_expires_in")),
                    (String) response.getOrDefault("token_type", "Bearer")
            );
        } catch (Exception e) {
            log.error("[KC {}] Token exchange failed: {}", flowType, e.getMessage());
            throw new IllegalStateException("Failed to exchange code with Keycloak", e);
        }
    }
}
