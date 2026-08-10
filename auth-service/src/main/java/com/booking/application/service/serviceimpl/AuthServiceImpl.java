package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.*;
import com.booking.application.port.out.*;
import com.booking.application.port.out.KeycloakAdminPort.KcUserInfo;
import com.booking.application.service.*;
import com.booking.application.validator.UserValidator;
import com.booking.domain.enums.DeactivationReason;
import com.booking.domain.event.UserRegisteredEvent;
import com.booking.domain.exception.*;
import com.booking.domain.model.Role;
import com.booking.domain.model.Token;
import com.booking.domain.model.User;
import com.booking.domain.model.UserKcLink;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.presentation.mapper.UserMapper;
import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.presentation.response.RegisterResponse;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements
        AuthService,
        LoginUseCase,
        RegisterUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenService tokenService;
    private final TokenCacheService tokenCacheService;
    private final PasswordService passwordService;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserMapper mapper;
    private final TwoFactorService twoFactorService;
    private final SystemParamService systemParamService;
    private final JwtService jwtService;

    private final UserValidator userValidator;
    private final DomainEventPublisher eventPublisher;

    // ═══ MỚI — KC sync ═══
    private final KeycloakAdminPort kcAdminClient;
    private final UserKcLinkRepositoryPort kcLinkRepo;

    private static final int REFRESH_TTL_DAYS = 7;

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public LoginResponse login(LoginRequest request,
                               String ipAddress,
                               String userAgent) {

        String username = request.getUsername();
        log.info("[Auth] Login attempt: {}", MaskUtil.maskUsername(username));

        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[Auth] User not found: {}", MaskUtil.maskUsername(username));
                    return new AuthException(ErrorCode.AUTH_001, ErrorCode.AUTH_001_MSG);
                });

        if (user.isAccountLocked()) {
            log.warn("[Auth] Account locked: {}", MaskUtil.maskUsername(username));
            throw new AuthException(ErrorCode.AUTH_002, ErrorCode.AUTH_002_MSG);
        }

        if (user.getPasswordHash() == null) {
            log.warn("[Auth] Passwordless SSO user attempted form login: {}",
                    MaskUtil.maskUsername(username));
            throw new AuthException(ErrorCode.AUTH_014, ErrorCode.AUTH_014_MSG);
        }

        boolean passwordOk = passwordService.verify(
                request.getPassword(),
                user.getPasswordHash(),
                user.getPasswordSalt(),
                user.getUsername()
        );

        if (!passwordOk) {
            user.incrementFailedAttempts();
            int maxAttempts = systemParamService.getIntValue("MAX_LOGIN_ATTEMPTS", 5);
            int lockMinutes = systemParamService.getIntValue("LOCK_DURATION_MINUTES", 15);

            if (user.getFailedAttempts() >= maxAttempts) {
                user.lockUntil(ZonedDateTime.now().plusMinutes(lockMinutes));
                log.warn("[Auth] Account auto-locked: {}", MaskUtil.maskUsername(username));
            }
            userRepositoryPort.save(user);
            throw new AuthException(ErrorCode.AUTH_001, ErrorCode.AUTH_001_MSG);
        }

        syncLocalUserToKeycloak(user, request.getPassword());

        user.resetFailedAttempts();
        userRepositoryPort.save(user);

        User userWithRoles = userRepositoryPort
                .findByIdWithRoles(user.getId())
                .orElse(user);

        if (userWithRoles.isTwoFactorEnabled()) {
            log.info("[Auth] 2FA required for: {}", MaskUtil.maskUsername(username));
            String mfaSessionToken = UUID.randomUUID().toString();
            tokenCacheService.saveMfaSession(mfaSessionToken, userWithRoles.getId().toString());

            return LoginResponse.builder()
                    .twoFactorRequired(true)
                    .mfaSessionToken(mfaSessionToken)
                    .build();
        }

        int killed = tokenRepositoryPort.deactivateAllByUserId(
                userWithRoles.getId(),
                DeactivationReason.NEW_LOGIN.name()
        );
        log.info("[Auth] Deactivated {} old token(s) for user {}",
                killed, MaskUtil.maskUsername(username));

        String jti = UUID.randomUUID().toString();
        String accessToken = jwtService.generateToken(userWithRoles, jti);
        String refreshToken = jwtService.generateRefreshToken(userWithRoles, jti);

        String tokenHash = jwtService.hashToken(refreshToken);
        Token tokenEntity = new Token();
        tokenEntity.setUser(userWithRoles);
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setJti(jti);
        tokenEntity.setActive(true);
        tokenEntity.setIpAddress(ipAddress);
        tokenEntity.setUserAgent(userAgent);
        tokenEntity.setCreatedAt(ZonedDateTime.now());
        tokenEntity.setExpiresAt(ZonedDateTime.now().plusDays(REFRESH_TTL_DAYS));
        tokenRepositoryPort.save(tokenEntity);

        log.info("[Auth] Login successful: {} (jti: {})",
                MaskUtil.maskUsername(username), jti);

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .username(userWithRoles.getUsername())
                .email(userWithRoles.getEmail())
                .roles(userWithRoles.getRoles().stream().map(Role::getCode).toList())
                .timezone(userWithRoles.getTimezone())
                .twoFactorRequired(false)
                .phoneRequired(isPhoneRequired(userWithRoles))
                .passwordRequired(false)
                .firstName(userWithRoles.getFirstName())
                .lastName(userWithRoles.getLastName())
                .build();
    }

    private boolean isPhoneRequired(User user) {
        String phone = user.getPhone();
        return phone == null || phone.isBlank();
    }

    // ═══════════════════════════════════════════════════════════
    // REGISTER — save local + sync KC
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();

        log.info("[Auth] Register attempt: {}", MaskUtil.maskUsername(username));

        userValidator.validateUsername(username);
        userValidator.validateEmail(email);
        userValidator.validatePassword(request.getPassword());

        if (userRepositoryPort.existsByUsername(username)) {
            log.warn("[Auth] Username exists: {}", MaskUtil.maskUsername(username));
            throw new UserException(ErrorCode.USR_002, ErrorCode.USR_002_MSG);
        }

        if (userRepositoryPort.existsByEmail(email)) {
            log.warn("[Auth] Email exists: {}", MaskUtil.maskEmail(email));
            throw new UserException(ErrorCode.USR_003, ErrorCode.USR_003_MSG);
        }

        UUID userId = UUID.randomUUID();
        PasswordService.HashedPassword hashed =
                passwordService.hash(request.getPassword(), username, userId.toString());

        User user = mapper.toEntity(request);
        user.setId(userId);
        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        if (user.getTimezone() == null || user.getTimezone().isBlank()) {
            user.setTimezone("UTC");
        }

        Role userRole = roleRepositoryPort.findByCode("USER")
                .orElseThrow(() -> new InfrastructureException(
                        ErrorCode.CMN_001, ErrorCode.CMN_001_MSG));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepositoryPort.save(user);
        String kcUserId = syncLocalUserToKeycloak(savedUser, request.getPassword());

        log.info("[Auth] Register successful: username={}, kcUserId={}, sync={}",
                MaskUtil.maskUsername(username), kcUserId, kcUserId != null ? "SYNCED" : "FAILED");
        eventPublisher.publish(new UserRegisteredEvent(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail()
        ));

        return mapper.toResponse(savedUser);
    }

    private String syncLocalUserToKeycloak(User user, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return null;
        }

        try {
            KcUserInfo kcUser = kcAdminClient.findUserByUsername(user.getUsername());
            if (kcUser == null) {
                kcUser = kcAdminClient.findUserByEmail(user.getEmail());
            }

            String kcUserId;
            if (kcUser == null) {
                kcUserId = kcAdminClient.createUser(user.getUsername(), user.getEmail(), rawPassword, true);
            } else {
                kcUserId = kcUser.id();
                kcAdminClient.updateUserProfile(
                        kcUserId,
                        user.getUsername(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName()
                );
                kcAdminClient.resetPassword(kcUserId, rawPassword);
            }

            saveKcLink(user.getId(), kcUserId, "SYNCED");
            return kcUserId;
        } catch (Exception e) {
            log.warn("[Auth] KC sync failed for local user {}: {}",
                    MaskUtil.maskUsername(user.getUsername()), e.getMessage());
            markKcSyncFailed(user.getId());
            return null;
        }
    }

    private void markKcSyncFailed(UUID userId) {
        UserKcLink link = kcLinkRepo.findByUserId(userId).orElseGet(UserKcLink::new);
        link.setUserId(userId);
        if (link.getKcUserId() == null) {
            link.setAuthSource("LOCAL");
            link.setKcSyncedAt(null);
        }
        link.setSyncStatus("FAILED");
        link.setSyncVersion(link.getSyncVersion() + 1);
        kcLinkRepo.save(link);
    }

    private void saveKcLink(UUID userId, String kcUserId, String syncStatus) {
        UserKcLink link = kcLinkRepo.findByUserId(userId).orElseGet(UserKcLink::new);
        link.setUserId(userId);
        link.setKcUserId(kcUserId);
        link.setKcProvider(null);
        link.setAuthSource(kcUserId != null ? "LINKED" : "LOCAL");
        link.setKcSyncedAt(kcUserId != null ? ZonedDateTime.now() : null);
        link.setSyncStatus(syncStatus);
        link.setSyncVersion(link.getSyncVersion() + 1);
        kcLinkRepo.save(link);
    }
}
