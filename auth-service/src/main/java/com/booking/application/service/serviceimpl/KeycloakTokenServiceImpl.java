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
 * KeycloakTokenServiceImpl = implement KeycloakTokenService
 * Verify id_token signature + claims từ Keycloak
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class KeycloakTokenServiceImpl implements KeycloakTokenService {

    private final KeycloakJwksPort jwksPort;
    private final AppProperties appProperties;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public IdTokenClaims verifyIdToken(String idToken) {
        try {
            // 1. Parse header để lấy kid
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = mapper.readTree(headerJson);
            String kid = header.get("kid").asText();
            String alg = header.get("alg").asText();

            // 2. Parse claims (chưa verify)
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = mapper.readTree(payloadJson);

            // 3. Verify expiration
            long exp = claims.get("exp").asLong();
            if (Instant.now().getEpochSecond() >= exp) {
                throw new IllegalStateException("id_token expired");
            }

            // 4. Verify issuer
            String iss = claims.get("iss").asText();
            String expectedIssuer = String.format(
                "%s/realms/%s",
                appProperties.getKeycloak().getUrl(),
                appProperties.getKeycloak().getRealm()
            );
            if (!expectedIssuer.equals(iss)) {
                throw new IllegalStateException(
                    "Invalid issuer: " + iss + " (expected: " + expectedIssuer + ")");
            }

            // 5. Verify audience (aud phải chứa FE client_id)
            JsonNode audNode = claims.get("aud");
            String feClientId = appProperties.getKeycloak().getFeClientId();
            boolean audOk = false;
            if (audNode.isArray()) {
                for (JsonNode a : audNode) {
                    if (feClientId.equals(a.asText())) { audOk = true; break; }
                }
            } else if (audNode.isTextual()) {
                audOk = feClientId.equals(audNode.asText());
            }
            if (!audOk) {
                throw new IllegalStateException(
                    "Invalid audience: " + audNode + " (expected to contain: " + feClientId + ")");
            }

            // 6. Verify signature qua JWKS
            PublicKey publicKey = jwksPort.getPublicKey(kid);
            if (publicKey == null) {
                throw new IllegalStateException("Public key not found for kid: " + kid);
            }
            if (!verifySignature(idToken, publicKey, alg)) {
                throw new IllegalStateException("Invalid id_token signature");
            }

            // 7. Extract thông tin user
            String sub = claims.get("sub").asText();
            String email = claims.has("email") ? claims.get("email").asText() : null;
            boolean emailVerified = claims.has("email_verified")
                && claims.get("email_verified").asBoolean();
            String username = claims.has("preferred_username")
                ? claims.get("preferred_username").asText()
                : (email != null ? email.split("@")[0] : sub);
            String firstName = claims.has("given_name")
                ? claims.get("given_name").asText() : null;
            String lastName = claims.has("family_name")
                ? claims.get("family_name").asText() : null;

            // 8. Extract roles từ realm_access.roles
            List<String> roles = new ArrayList<>();
            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    realmAccess.get("roles").forEach(
                        r -> roles.add(r.asText())
                    );
                }
            }

            log.info("[KC] id_token verified: sub={}, email={}, roles={}",
                sub, email, roles);

            return new IdTokenClaims(sub, email, emailVerified, username,
                firstName, lastName, roles, exp);
        } catch (Exception e) {
            log.error("[KC] id_token verification failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid id_token: " + e.getMessage(), e);
        }
    }

    /**
     * Verify signature manually bằng Java Security
     */
    private boolean verifySignature(String jwt, PublicKey publicKey, String alg) throws Exception {
        String[] parts = jwt.split("\\.");
        String data = parts[0] + "." + parts[1];
        byte[] signature = Base64.getUrlDecoder().decode(parts[2]);

        String javaAlg = switch (alg) {
            case "RS256" -> "SHA256withRSA";
            case "RS384" -> "SHA384withRSA";
            case "RS512" -> "SHA512withRSA";
            default -> throw new IllegalArgumentException("Unsupported alg: " + alg);
        };

        Signature sig = Signature.getInstance(javaAlg);
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        return sig.verify(signature);
    }
}
