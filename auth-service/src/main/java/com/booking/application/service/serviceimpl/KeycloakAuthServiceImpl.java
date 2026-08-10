package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ExchangeKeycloakCodeUseCase;
import com.booking.application.port.out.KeycloakTokenPort;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserKcLinkRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.KeycloakTokenService;
import com.booking.application.service.KeycloakTokenService.IdTokenClaims;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.model.Role;
import com.booking.domain.model.Token;
import com.booking.domain.model.User;
import com.booking.domain.model.UserKcLink;
import com.booking.infrastructure.config.AppProperties;
import com.booking.presentation.request.ExchangeCodeRequest;
import com.booking.presentation.response.LoginResponse;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * KeycloakAuthServiceImpl — Phase C v2: tách bảng user_kc_links
 *
 * Flow:
 *   1. Exchange code → KC tokens
 *   2. Verify id_token
 *   3. Tìm KC link → 3 cases:
 *      A. Link tồn tại (kc_user_id match) → login
 *      B. User email match, chưa link      → link + login
 *      C. Không tìm thấy                   → tạo user + link + login
 *   4. Generate JWT (access + refresh)
 *   5. Save KC tokens
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class KeycloakAuthServiceImpl implements ExchangeKeycloakCodeUseCase {

    private final KeycloakTokenPort         kcTokenPort;
    private final KeycloakTokenService      kcTokenService;
    private final UserRepositoryPort        userRepo;
    private final UserKcLinkRepositoryPort  kcLinkRepo;      // ← MỚI
    private final RoleRepositoryPort        roleRepo;
    private final TokenRepositoryPort       tokenRepositoryPort;
    private final JwtService                jwtService;
    private final AppProperties             appProperties;

    private static final int REFRESH_TTL_DAYS = 7;

    private record UpsertResult(User user, boolean isNewUser) {}

    @Override
    public String buildAuthorizationUrl(String state, String provider) {
        AppProperties.KeycloakProps kc = appProperties.getKeycloak();

        String authEndpoint = String.format(
                "%s/realms/%s/protocol/openid-connect/auth",
                kc.getIssuerUrl(),
                kc.getRealm()
        );

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(authEndpoint)
                .queryParam("client_id", kc.getClientId())
                .queryParam("redirect_uri", kc.getBffCallbackUrl())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("prompt", "login")
                .queryParam("kc_action", "authenticate");

        if (provider != null && !provider.isBlank()) {
            builder.queryParam("kc_idp_hint", provider.trim().toLowerCase());
        }

        return builder
                .build()
                .encode()
                .toUriString();
    }

    @Override
    @Transactional
    public LoginResponse handleCallback(String code, String ipAddress, String userAgent) {
        log.info("[BFF] Callback: exchanging code from IP={}", ipAddress);

        AppProperties.KeycloakProps kc = appProperties.getKeycloak();

        KeycloakTokenPort.TokenResponse kcTokens = kcTokenPort.exchangeCodeConfidential(
                code, kc.getBffCallbackUrl()
        );

        if (kcTokens.idToken() == null) {
            throw new IllegalStateException(ErrorCode.AUTH_013 + ": Keycloak did not return id_token");
        }

        IdTokenClaims claims = kcTokenService.verifyIdToken(kcTokens.idToken());
        log.info("[BFF] id_token verified: sub={}, email={}",
                claims.sub(), MaskUtil.maskEmail(claims.email()));

        return processKcLogin(claims, null, ipAddress, userAgent);
    }
    @Override
    @Transactional
    public LoginResponse exchange(ExchangeCodeRequest request,
                                  String ipAddress, String userAgent) {
        log.info("[KC Auth] Exchange code from IP={}", ipAddress);

        // Bước 1: Exchange code → KC tokens
        KeycloakTokenPort.TokenResponse kcTokens = kcTokenPort.exchangeCode(
                request.getCode(), request.getCodeVerifier(), request.getRedirectUri());
        if (kcTokens.idToken() == null) {
            throw new IllegalStateException(ErrorCode.AUTH_013 + ": Keycloak did not return id_token");
        }

        // Bước 2: Verify id_token
        IdTokenClaims claims = kcTokenService.verifyIdToken(kcTokens.idToken());
        log.info("[KC Auth] id_token verified: sub={}, email={}, username={}",
                claims.sub(), MaskUtil.maskEmail(claims.email()), claims.preferredUsername());

        return processKcLogin(claims, request.getTimeZone(), ipAddress, userAgent);
    }

    // ══════════════════════════════════════════════════════════

    private UpsertResult upsertUserFromKc(IdTokenClaims claims, String timeZone) {
        String rawEmail = claims.email();
        if (rawEmail == null || rawEmail.isBlank()) {
            // Federation provider không trả email → extract local user ID từ sub
            // Format: f:<provider_id>:<local_user_id>
            String sub = claims.sub();
            log.info("[KC Auth] Email null, trying Federation fallback: sub={}", sub);

            String localIdStr = sub;
            if (sub.startsWith("f:")) {
                // Extract UUID cuối: f:xxxx:a029c540-6b84-4497-b092-a2456ceadd96
                String[] parts = sub.split(":");
                localIdStr = parts[parts.length - 1];
            }

            try {
                UUID localUserId = UUID.fromString(localIdStr);
                Optional<User> byId = userRepo.findById(localUserId);
                if (byId.isPresent()) {
                    User found = byId.get();
                    log.info("[KC Auth] Federation fallback OK: userId={}, email={}",
                            found.getId(), MaskUtil.maskEmail(found.getEmail()));

                    // Tạo KC link
                    Optional<UserKcLink> existingLink = kcLinkRepo.findByUserId(found.getId());
                    if (existingLink.isEmpty()) {
                        UserKcLink newLink = new UserKcLink(found.getId(), sub, claims.provider(), "SSO");
                        kcLinkRepo.save(newLink);
                    }

                    return new UpsertResult(found, false);
                }
            } catch (IllegalArgumentException ignored) {
            }

            throw new IllegalStateException(ErrorCode.AUTH_013 + ": " + ErrorCode.AUTH_013_MSG);
        }
        if (!claims.emailVerified()) {
            // Federation user có thể không set emailVerified trong token
            try {
                UUID localUserId = UUID.fromString(claims.sub());
                Optional<User> byId = userRepo.findById(localUserId);
                if (byId.isPresent() && byId.get().isEmailVerified()) {
                    log.info("[KC Auth] Federation user email verified in local DB: sub={}", claims.sub());
                    UserKcLink newLink = new UserKcLink(byId.get().getId(), claims.sub(), claims.provider(), "SSO");
                    kcLinkRepo.save(newLink);
                    return new UpsertResult(byId.get(), false);
                }
            } catch (IllegalArgumentException ignored) {}
            log.warn("[KC Auth] id_token email is not marked verified: email={}",
                    MaskUtil.maskEmail(rawEmail));
        }

        String email = rawEmail.toLowerCase().trim();
        String kcUserId = claims.sub();

        // ── Case A: tìm KC link theo sub ────────────────────
        Optional<UserKcLink> existingLink = kcLinkRepo.findByKcUserId(kcUserId);
        if (existingLink.isPresent()) {
            log.info("[KC Auth] Case A — existing KC link: email={}", MaskUtil.maskEmail(email));
            UserKcLink link = existingLink.get();
            link.updateSync();
            if (link.getKcProvider() == null && claims.provider() != null) {
                link.setKcProvider(claims.provider());
            }
            kcLinkRepo.save(link);
            User found = userRepo.findById(link.getUserId())
                    .orElseThrow(() -> new IllegalStateException("User not found for KC link"));
            return new UpsertResult(found, false);
        }
        // ── Case A2: Federation dùng local user ID làm sub ──
        try {
            String localIdStr = kcUserId;
            if (kcUserId.startsWith("f:")) {
                String[] parts = kcUserId.split(":");
                localIdStr = parts[parts.length - 1];
            }
            UUID localUserId = UUID.fromString(localIdStr);
            Optional<User> byId = userRepo.findById(localUserId);
            if (byId.isPresent()) {
                log.info("[KC Auth] Case A2 - Federation user found by local ID: email={}", MaskUtil.maskEmail(email));
                User found = byId.get();
                UserKcLink newLink = new UserKcLink(found.getId(), kcUserId, claims.provider(), "SSO");
                kcLinkRepo.save(newLink);
                return new UpsertResult(found, false);
            }
        } catch (IllegalArgumentException ignored) {
        }


        // ── Case B/C: tìm user theo email ───────────────────

        Optional<User> byEmail = userRepo.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            return new UpsertResult(linkOrRejectLocalUser(byEmail.get(), kcUserId, claims, email), false);
        }

        if (!claims.emailVerified()) {
            throw new IllegalStateException(ErrorCode.AUTH_010 + ": " + ErrorCode.AUTH_010_MSG);
        }

        // ── Case C: tạo mới ─────────────────────────────────
        log.info("[KC Auth] Case C — auto-create: email={}", MaskUtil.maskEmail(email));
        return new UpsertResult(createFromKc(email, kcUserId, claims, timeZone), true);
    }

    /**
     * Case B: Link local user với KC
     */
    private User linkOrRejectLocalUser(User existing, String kcUserId,
                                       IdTokenClaims claims, String email) {
        User verifiedUser = ensureLocalEmailVerifiedForSso(existing, claims, email);
        // Hijack check: user đã link KC khác?
        Optional<UserKcLink> existingLink = kcLinkRepo.findByUserId(verifiedUser.getId());
        if (existingLink.isPresent() && !kcUserId.equals(existingLink.get().getKcUserId())) {
            log.info("[KC Auth] Case B — updating KC link: email={}, old_kc={}, new_kc={}",
                    MaskUtil.maskEmail(email), existingLink.get().getKcUserId(), kcUserId);
            UserKcLink link = existingLink.get();
            link.setKcUserId(kcUserId);
            link.setKcProvider(claims.provider());
            link.updateSync();
            kcLinkRepo.save(link);
            return verifiedUser;
        }

        // Email chưa verify
        if (!verifiedUser.isEmailVerified()) {
            log.warn("[KC Auth] Case B rejected — email not verified: email={}", MaskUtil.maskEmail(email));
            throw new IllegalStateException(ErrorCode.AUTH_011 + ": " + ErrorCode.AUTH_011_MSG);
        }

        // Link: tạo record user_kc_links
        log.info("[KC Auth] Case B — linking: email={}", MaskUtil.maskEmail(email));
        UserKcLink link = new UserKcLink();
        link.setUserId(verifiedUser.getId());
        link.setKcUserId(kcUserId);
        link.setKcProvider(claims.provider());
        link.setAuthSource(verifiedUser.getPasswordHash() != null ? "LINKED" : "KEYCLOAK");
        link.setKcSyncedAt(ZonedDateTime.now());
        link.setSyncStatus("SYNCED");
        link.setSyncVersion(1L);
        kcLinkRepo.save(link);

        return verifiedUser;
    }

    private User ensureLocalEmailVerifiedForSso(User user, IdTokenClaims claims, String email) {
        if (user.isEmailVerified()) {
            return user;
        }

        if (!claims.emailVerified()) {
            log.warn("[KC Auth] Case B rejected - email not verified: email={}", MaskUtil.maskEmail(email));
            throw new IllegalStateException(ErrorCode.AUTH_011 + ": " + ErrorCode.AUTH_011_MSG);
        }

        User userWithRoles = userRepo.findByIdWithRoles(user.getId()).orElse(user);
        userWithRoles.setEmailVerified(true);
        User saved = userRepo.save(userWithRoles);

        log.info("[KC Auth] Local email verified from SSO provider: userId={}, email={}",
                saved.getId(), MaskUtil.maskEmail(email));

        return saved;
    }

    /**
     * Case C: Tạo user mới + KC link
     */
    private User createFromKc(String email, String kcUserId,
                              IdTokenClaims claims, String timeZone) {
        // Username: dùng KC preferred_username, fallback email prefix
        String username = email;

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(null);
        user.setPasswordSalt(null);
        user.setEmailVerified(true);
        user.setActive(true);
        user.setTimezone(timeZone != null ? timeZone : "UTC");

        Set<Role> roles = resolveRoles(claims);
        user.setRoles(roles);

        try {
            User saved = userRepo.save(user);

            // Tạo KC link record
            UserKcLink link = new UserKcLink();
            link.setUserId(saved.getId());
            link.setKcUserId(kcUserId);
            link.setKcProvider(claims.provider());
            link.setAuthSource("KEYCLOAK");
            link.setKcSyncedAt(ZonedDateTime.now());
            link.setSyncStatus("SYNCED");
            link.setSyncVersion(1L);
            kcLinkRepo.save(link);

            log.info("[KC Auth] Case C — created: email={}, username={}", MaskUtil.maskEmail(email), username);
            return saved;

        } catch (DataIntegrityViolationException ex) {
            log.warn("[KC Auth] Race condition, retrying: sub={}", kcUserId);
            return kcLinkRepo.findByKcUserId(kcUserId)
                    .flatMap(link -> userRepo.findById(link.getUserId()))
                    .orElseThrow(() -> new IllegalStateException("Race condition: " + ex.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private String resolveUsername(String base) {
        base = base.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (base.isBlank()) base = "user";
        if (!userRepo.existsByUsername(base)) return base;
        for (int i = 1; i <= 999; i++) {
            String candidate = base + i;
            if (!userRepo.existsByUsername(candidate)) return candidate;
        }
        return base + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private Set<Role> resolveRoles(IdTokenClaims claims) {
        Set<Role> roles = new HashSet<>();
        for (String roleCode : claims.roles()) {
            roleRepo.findByCode(roleCode).ifPresent(roles::add);
        }
        if (roles.isEmpty()) {
            roleRepo.findByCode("USER").ifPresent(roles::add);
        }
        return roles;
    }

    private boolean isPhoneRequired(User user) {
        return user.getPhone() == null || user.getPhone().isBlank();
    }

    private LoginResponse processKcLogin(IdTokenClaims claims, String timeZone,
                                         String ipAddress, String userAgent) {
        UpsertResult result = upsertUserFromKc(claims, timeZone);
        User user = result.user();
        boolean isNewUser = result.isNewUser();
        User userWithRoles = userRepo.findByIdWithRoles(user.getId()).orElse(user);

        int killed = tokenRepositoryPort.deactivateAllByUserId(userWithRoles.getId(), "NEW_LOGIN");
        if (killed > 0) log.info("[KC Auth] Killed {} old session(s)", killed);

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(userWithRoles, jti);
        String refreshToken = jwtService.generateRefreshToken(userWithRoles, jti);

        Token tokenEntity = new Token();
        tokenEntity.setUser(userWithRoles);
        tokenEntity.setTokenHash(jwtService.hashToken(refreshToken));
        tokenEntity.setJti(jti);
        tokenEntity.setActive(true);
        tokenEntity.setIpAddress(ipAddress);
        tokenEntity.setUserAgent(userAgent);
        tokenEntity.setCreatedAt(ZonedDateTime.now());
        tokenEntity.setExpiresAt(ZonedDateTime.now().plusDays(REFRESH_TTL_DAYS));
        tokenRepositoryPort.save(tokenEntity);

        boolean passwordRequired = userWithRoles.getPasswordHash() == null;

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(userWithRoles.getUsername())
                .email(userWithRoles.getEmail())
                .roles(userWithRoles.getRoles().stream().map(Role::getCode).toList())
                .timezone(userWithRoles.getTimezone())
                .twoFactorRequired(false)
                .phoneRequired(isPhoneRequired(userWithRoles))
                .passwordRequired(userWithRoles.getPasswordHash() == null)
                .firstName(userWithRoles.getFirstName())
                .lastName(userWithRoles.getLastName())
                .build();
    }
}
