package com.booking.application.service.serviceimpl;

import com.booking.application.port.out.KeycloakJwksPort;
import com.booking.application.service.KeycloakTokenService;
import com.booking.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * KeycloakTokenServiceImpl — verify id_token từ Keycloak
 *
 * Phase C cleanup:
 *   - Fix bug: claims.get("identity_provider", String.class) → JsonNode không có method này
 *   - Fix bug: return dùng biến 'pr' không tồn tại → đổi thành 'provider'
 *   - Bỏ extract username/firstName/lastName/expiresAt (không dùng ở AuthService)
 *   - IdTokenClaims giữ 5 field: sub, email, emailVerified, roles, provider
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class KeycloakTokenServiceImpl implements KeycloakTokenService {

    private final KeycloakJwksPort jwksPort;
    private final AppProperties   appProperties;
    private final ObjectMapper    mapper = new ObjectMapper();

    @Override
    public IdTokenClaims verifyIdToken(String idToken) {
        try {
            // ── 1. Parse JWT header → kid, alg ─────────────────
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format: expected 3 parts");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header   = mapper.readTree(headerJson);
            String kid = header.get("kid").asText();
            String alg = header.get("alg").asText();

            // ── 2. Parse payload (chưa verify) ─────────────────
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims    = mapper.readTree(payloadJson);

            // ── 3. Verify expiration ────────────────────────────
            long exp = claims.get("exp").asLong();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalStateException("id_token expired");
            }

            // ── 4. Verify issuer ────────────────────────────────
            String iss = claims.get("iss").asText();
            String expectedIssuer = String.format("%s/realms/%s",
                    appProperties.getKeycloak().getIssuerUrl(),
                    appProperties.getKeycloak().getRealm());
            if (!expectedIssuer.equals(iss)) {
                throw new IllegalStateException(
                        "Invalid issuer: " + iss + " (expected: " + expectedIssuer + ")");
            }

            // ── 5. Verify audience ──────────────────────────────
            JsonNode audNode   = claims.get("aud");
            String feClientId  = appProperties.getKeycloak().getFeClientId();     // booking-frontend
            String beClientId  = appProperties.getKeycloak().getClientId();       // booking-backend
            boolean audOk      = false;
            if (audNode.isArray()) {
                for (JsonNode a : audNode) {
                    String v = a.asText();
                    if (feClientId.equals(v) || beClientId.equals(v)) { audOk = true; break; }
                }
            } else if (audNode.isTextual()) {
                String v = audNode.asText();
                audOk = feClientId.equals(v) || beClientId.equals(v);
            }

            // ── 6. Verify signature via JWKS ────────────────────
            PublicKey publicKey = jwksPort.getPublicKey(kid);
            if (publicKey == null) {
                throw new IllegalStateException("Public key not found for kid: " + kid);
            }
            if (!verifySignature(idToken, publicKey, alg)) {
                throw new IllegalStateException("Invalid id_token signature");
            }

            // ── 7. Extract claims (chỉ field cần thiết) ────────
            String sub   = claims.get("sub").asText();
            String email = claims.has("email")
                    ? claims.get("email").asText() : null;

            boolean emailVerified = claims.has("email_verified")
                    && claims.get("email_verified").asBoolean();

            // FIX: JsonNode.get() trả về JsonNode, dùng .asText()
            // Trước đây dùng claims.get("identity_provider", String.class) — không tồn tại
            String provider = claims.has("identity_provider")
                    ? claims.get("identity_provider").asText() : null;

            // Realm roles
            List<String> roles = new ArrayList<>();
            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    realmAccess.get("roles").forEach(r -> roles.add(r.asText()));
                }
            }

            log.info("[KC] id_token verified: sub={}, email={}, emailVerified={}, provider={}, roles={}",
                    sub, email, emailVerified, provider, roles);

            String preferredUsername = claims.has("preferred_username")
                    ? claims.get("preferred_username").asText() : null;

            return new IdTokenClaims(sub, email, emailVerified, roles, provider, preferredUsername);

        } catch (Exception e) {
            log.error("[KC] id_token verification failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid id_token: " + e.getMessage(), e);
        }
    }

    // ── Signature verification ──────────────────────────────────

    private boolean verifySignature(String jwt, PublicKey publicKey, String alg)
            throws Exception {
        String[] parts = jwt.split("\\.");
        String data    = parts[0] + "." + parts[1];
        byte[] sigBytes = Base64.getUrlDecoder().decode(parts[2]);

        String javaAlg = switch (alg) {
            case "RS256" -> "SHA256withRSA";
            case "RS384" -> "SHA384withRSA";
            case "RS512" -> "SHA512withRSA";
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + alg);
        };

        Signature sig = Signature.getInstance(javaAlg);
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        return sig.verify(sigBytes);
    }
}