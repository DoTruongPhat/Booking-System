package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ExchangeKeycloakCodeUseCase;
import com.booking.application.port.out.*;
import com.booking.application.service.JwtService;
import com.booking.application.service.KeycloakTokenService;
import com.booking.application.service.KeycloakTokenService.IdTokenClaims;
import com.booking.application.service.SessionService;
import com.booking.domain.model.KcToken;
import com.booking.domain.model.Role;
import com.booking.domain.model.User;
import com.booking.domain.model.UserSession;
import com.booking.infrastructure.config.AppProperties;
import com.booking.presentation.request.ExchangeCodeRequest;
import com.booking.presentation.response.LoginResponse;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * KeycloakAuthService - xử lý Form B login
 *
 * Flow:
 * 1. FE gửi code + code_verifier (PKCE)
 * 2. BE gọi KC /token endpoint → access/refresh/id_token
 * 3. BE verify id_token qua JWKS
 * 4. Upsert user trong BE PostgreSQL (sync)
 * 5. Load roles của user
 * 6. Kill old sessions (single session)
 * 7. Tạo session mới + JWT nội bộ
 * 8. Lưu kc_refresh_token để logout sau
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class KeycloakAuthServiceImpl implements ExchangeKeycloakCodeUseCase {

    private final KeycloakTokenPort kcTokenPort;
    private final KeycloakTokenService kcTokenService;
    private final KcTokenRepositoryPort kcTokenRepo;
    private final UserRepositoryPort userRepo;
    private final RoleRepositoryPort roleRepo;
    private final SessionService sessionService;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public LoginResponse exchange(ExchangeCodeRequest request,
                                   String ipAddress, String userAgent) {
        log.info("[KC Auth] Exchange code request from IP: {}", ipAddress);

        // Bước 1: Gọi KC /token để exchange code
        KeycloakTokenPort.TokenResponse kcTokens = kcTokenPort.exchangeCode(
            request.getCode(),
            request.getCodeVerifier(),
            request.getRedirectUri()
        );

        if (kcTokens.idToken() == null) {
            throw new IllegalStateException("Keycloak did not return id_token");
        }

        // Bước 2: Verify id_token
        IdTokenClaims claims = kcTokenService.verifyIdToken(kcTokens.idToken());
        log.info("[KC Auth] id_token verified: sub={}, email={}",
            claims.sub(), MaskUtil.maskEmail(claims.email()));

        // Bước 3: Upsert user trong BE
        User user = upsertUserFromKc(claims);

        // Bước 4: Load user với roles
        User userWithRoles = userRepo.findByIdWithRoles(user.getId()).orElse(user);

        // Bước 5: Single Session - kill old sessions
        int killed = sessionService.killOldSessions(
            userWithRoles.getId(),
            UserSession.REASON_NEW_LOGIN
        );
        if (killed > 0) {
            log.info("[KC Auth] Killed {} old session(s) for user {}",
                killed, userWithRoles.getUsername());
        }

        // Bước 6: Tạo session mới
        String jti = UUID.randomUUID().toString();
        int ttl = appProperties.getKeycloak().getSessionTtl();
        sessionService.createSession(
            userWithRoles, jti,
            UserSession.SOURCE_KEYCLOAK,
            parseUserAgent(userAgent), ipAddress, userAgent, ttl
        );

        // Bước 7: Tạo JWT nội bộ
        String jwt = jwtService.generateToken(userWithRoles, jti);

        // Bước 8: Lưu KC tokens (để logout SSO sau này)
        saveKcTokens(userWithRoles.getId(), claims.sub(), kcTokens);

        // Bước 9: Trả response
        return LoginResponse.builder()
            .token(jwt)
            .username(userWithRoles.getUsername())
            .email(userWithRoles.getEmail())
            .roles(userWithRoles.getRoles().stream()
                .map(Role::getCode).toList())
            .timezone(userWithRoles.getTimezone())
            .twoFactorRequired(false)
            .build();
    }

    // ── User sync từ KC → BE ─────────────────────────────

    private User upsertUserFromKc(IdTokenClaims claims) {
        String kcUserId = claims.sub();
        String email = claims.email();

        // Tìm user theo kc_user_id trước (link trực tiếp)
        Optional<User> existing = userRepo.findByKcUserId(kcUserId);
        if (existing.isEmpty()) {
            // Nếu không tìm thấy theo kcUserId, tìm theo email
            // (case: user tạo Form A trước, chưa link KC)
            existing = userRepo.findByEmail(email);
        }

        if (existing.isPresent()) {
            User user = existing.get();
            user.setKcUserId(kcUserId);
            user.setKcSyncedAt(ZonedDateTime.now());
            user.setSyncStatus("SYNCED");
            user.setSyncVersion(user.getSyncVersion() + 1);

            // Update auth_source
            if ("LOCAL".equals(user.getAuthSource())) {
                user.setAuthSource("LINKED");
            } else if (user.getAuthSource() == null) {
                user.setAuthSource("KEYCLOAK");
            }

            // Update name nếu chưa có
            if (user.getEmail() != null && claims.username() != null) {
                // keep existing username
            }

            log.info("[KC Auth] User already exists, updated kc link: {}",
                MaskUtil.maskEmail(email));
            return userRepo.save(user);
        }

        // Tạo mới
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setUsername(claims.username() != null
            ? claims.username() : email.split("@")[0]);
        user.setKcUserId(kcUserId);
        user.setKcSyncedAt(ZonedDateTime.now());
        user.setSyncStatus("SYNCED");
        user.setSyncVersion(1L);
        user.setAuthSource("KEYCLOAK");
        user.setActive(true);
        user.setTimezone("UTC");

        // Gán roles mặc định dựa trên KC roles
        Set<Role> roles = new HashSet<>();
        for (String roleCode : claims.roles()) {
            roleRepo.findByCode(roleCode).ifPresent(roles::add);
        }
        // Fallback: gán USER nếu không match role nào
        if (roles.isEmpty()) {
            roleRepo.findByCode("USER").ifPresent(roles::add);
        }
        user.setRoles(roles);

        log.info("[KC Auth] Created new user from KC: {} with roles {}",
            MaskUtil.maskEmail(email), claims.roles());
        return userRepo.save(user);
    }

    private void saveKcTokens(UUID userId, String kcUserId,
                              KeycloakTokenPort.TokenResponse tokens) {
        KcToken existing = kcTokenRepo.findByUserId(userId).orElse(null);
        if (existing == null) {
            existing = new KcToken();
            existing.setUserId(userId);
        }

        existing.setKcUserId(kcUserId);
        existing.setKcAccessToken(tokens.accessToken());
        existing.setKcRefreshToken(tokens.refreshToken());
        existing.setAccessTokenExpiresAt(
            ZonedDateTime.now().plusSeconds(tokens.expiresIn())
        );
        existing.setRefreshTokenExpiresAt(
            ZonedDateTime.now().plusSeconds(tokens.refreshExpiresIn())
        );
        existing.setLastRefreshedAt(ZonedDateTime.now());

        kcTokenRepo.save(existing);
        log.info("[KC Auth] Saved KC tokens for user {}", userId);
    }

    private String parseUserAgent(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Chrome")) return "Chrome / Desktop";
        if (ua.contains("Firefox")) return "Firefox / Desktop";
        if (ua.contains("Safari")) return "Safari / Desktop";
        return "Unknown";
    }
}
