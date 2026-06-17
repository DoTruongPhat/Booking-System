package com.booking.infrastructure.external.keycloak;

import com.booking.application.port.out.KeycloakJwksPort;
import com.booking.infrastructure.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * KeycloakJwksClient - cache public keys từ Keycloak JWKS endpoint
 * Implement KeycloakJwksPort
 *
 * Keycloak rotate keys thường xuyên → cần cache nhưng vẫn refresh định kỳ
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class KeycloakJwksClient implements KeycloakJwksPort {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ReentrantLock lock = new ReentrantLock();

    private Map<String, PublicKey> keyCache = new HashMap<>();
    private Instant cacheExpiry = Instant.EPOCH;

    @PostConstruct
    public void init() {
        try {
            refreshCache();
        } catch (Exception e) {
            log.warn("[KC] Initial JWKS load failed: {}", e.getMessage());
        }
    }

    @Override
    public PublicKey getPublicKey(String kid) {
        // Cache expired → refresh
        if (Instant.now().isAfter(cacheExpiry)) {
            lock.lock();
            try {
                if (Instant.now().isAfter(cacheExpiry)) {
                    refreshCache();
                }
            } finally {
                lock.unlock();
            }
        }

        PublicKey key = keyCache.get(kid);
        if (key == null) {
            // Key không có trong cache → có thể vừa rotate → force refresh
            log.warn("[KC] Key not found in cache for kid: {}, forcing refresh", kid);
            refreshCache();
            key = keyCache.get(kid);
        }
        return key;
    }

    @Override
    public Map<String, PublicKey> getAllKeys() {
        if (Instant.now().isAfter(cacheExpiry)) {
            refreshCache();
        }
        return new HashMap<>(keyCache);
    }

    @Override
    public void refreshCache() {
        lock.lock();
        try {
            String url = String.format(
                "%s/realms/%s/protocol/openid-connect/certs",
                appProperties.getKeycloak().getUrl(),
                appProperties.getKeycloak().getRealm()
            );

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.error("[KC] Failed to fetch JWKS: status={}", response.getStatusCode());
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            @SuppressWarnings("unchecked")
            var keys = (java.util.List<Map<String, Object>>) body.get("keys");

            Map<String, PublicKey> newCache = new HashMap<>();
            for (Map<String, Object> key : keys) {
                try {
                    String kid = (String) key.get("kid");
                    String kty = (String) key.get("kty");
                    if (!"RSA".equals(kty)) continue;  // Chỉ support RSA

                    String n = (String) key.get("n");
                    String e = (String) key.get("e");
                    PublicKey publicKey = buildRsaPublicKey(n, e);

                    newCache.put(kid, publicKey);
                } catch (Exception ex) {
                    log.warn("[KC] Failed to parse key {}: {}",
                        key.get("kid"), ex.getMessage());
                }
            }

            this.keyCache = newCache;
            this.cacheExpiry = Instant.now().plusSeconds(
                appProperties.getKeycloak().getJwksCacheTtl()
            );

            log.info("[KC] JWKS cache refreshed: {} keys, TTL={}s",
                newCache.size(), appProperties.getKeycloak().getJwksCacheTtl());
        } catch (Exception e) {
            log.error("[KC] Failed to refresh JWKS cache: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Build RSA PublicKey từ modulus (n) và exponent (e) trong JWKS
     */
    private PublicKey buildRsaPublicKey(String n, String e) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}
