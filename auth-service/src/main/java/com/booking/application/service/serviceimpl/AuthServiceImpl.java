package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.*;
import com.booking.application.port.out.DomainEventPublisher;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.*;
import com.booking.application.validator.UserValidator;
import com.booking.domain.event.UserRegisteredEvent;
import com.booking.domain.exception.*;
import com.booking.domain.model.Role;
import com.booking.domain.model.User;
import com.booking.presentation.mapper.UserMapper;
import com.booking.shared.util.MaskUtil;
import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.presentation.response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements
        AuthService,
        LoginUseCase,
        RegisterUseCase{

    private final UserRepositoryPort userRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenService tokenService;
    private final TokenCacheService tokenCacheService;
    private final PasswordService passwordService;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserMapper mapper;
    private final TwoFactorService twoFactorService;
    private final SystemParamService systemParamService;
    private final SessionService sessionService;
    private final JwtService jwtService;

    private final UserValidator userValidator;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public LoginResponse login(LoginRequest request,
                               String ipAddress,
                               String userAgent) {

        String username = request.getUsername();
        log.info("[Auth] Login attempt: {}", MaskUtil.maskUsername(username));

        // Bước 1: Tìm user trong DB
        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[Auth] User not found: {}", MaskUtil.maskUsername(username));
                    return new AuthException(
                            ErrorCode.AUTH_001,
                            ErrorCode.AUTH_001_MSG);
                });

        // Bước 2: Kiểm tra account
        if (user.isAccountLocked()) {
            log.warn("[Auth] Account locked: {}", MaskUtil.maskUsername(username));
            throw new AuthException(
                    ErrorCode.AUTH_002,
                    ErrorCode.AUTH_002_MSG);
        }

        // Bước 3: Verify password local (BCrypt)
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
                log.warn("[Auth] Account auto-locked: {}",
                        MaskUtil.maskUsername(username));
            }
            userRepositoryPort.save(user);
            throw new AuthException(
                    ErrorCode.AUTH_001,
                    ErrorCode.AUTH_001_MSG);
        }

        // Bước 4: KC OK → reset failed attempts
        user.resetFailedAttempts();
        userRepositoryPort.save(user);

        User userWithRoles = userRepositoryPort
                .findByIdWithRoles(user.getId())
                .orElse(user);

        // Bước 4.5: Kiểm tra 2FA
        if (userWithRoles.isTwoFactorEnabled()) {
            log.info("[Auth] 2FA required for: {}",
                    MaskUtil.maskUsername(username));

            String mfaSessionToken = UUID.randomUUID().toString();
            tokenCacheService.saveMfaSession(
                    mfaSessionToken,
                    userWithRoles.getId().toString());

            return LoginResponse.builder()
                    .twoFactorRequired(true)
                    .mfaSessionToken(mfaSessionToken)
                    .build();
        }

        // Bước 5: Single Session - Kill tất cả session cũ của user
        // → Nếu user login ở nơi khác → session cũ bị kill ngay lập tức
        int killed = sessionService.killOldSessions(
            userWithRoles.getId(),
            com.booking.domain.model.UserSession.REASON_NEW_LOGIN
        );
        log.info("[Auth] Killed {} old session(s) for user {}",
            killed, MaskUtil.maskUsername(username));

        // Bước 6: Tạo JWT nội bộ + session tracking
        // → JWT có jti match với user_sessions.jti
        // → Nếu session bị kill → JWT cũng không dùng được nữa
        String jti = UUID.randomUUID().toString();
        String deviceInfo = parseUserAgent(userAgent);
        int ttlSeconds = 3600; // 1 giờ - sẽ đổi khi có refresh token

        com.booking.domain.model.UserSession newSession =
            sessionService.createSession(
                userWithRoles, jti,
                com.booking.domain.model.UserSession.SOURCE_LOCAL,
                deviceInfo, ipAddress, userAgent, ttlSeconds
            );

        // Tạo JWT với jti đã track
        String rawToken = jwtService.generateToken(userWithRoles, jti);

        log.info("[Auth] Login successful: {} (jti: {})",
            MaskUtil.maskUsername(username), jti);

        // Bước 7: Trả response
        return LoginResponse.builder()
                .token(rawToken)
                .username(userWithRoles.getUsername())
                .email(userWithRoles.getEmail())
                .roles(userWithRoles.getRoles().stream()
                        .map(r -> r.getCode())
                        .toList())
                .timezone(userWithRoles.getTimezone())
                .twoFactorRequired(false)
                .build();
    }

    /**
     * Parse user agent thành device info ngắn gọn
     * VD: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0"
     *   → "Chrome 120 / Windows"
     */
    private String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        // Simplified - production nên dùng ua-parser library
        if (userAgent.contains("Chrome")) return "Chrome / Desktop";
        if (userAgent.contains("Firefox")) return "Firefox / Desktop";
        if (userAgent.contains("Safari")) return "Safari / Desktop";
        if (userAgent.contains("Edge")) return "Edge / Desktop";
        if (userAgent.contains("Mobile")) return "Mobile Browser";
        return "Unknown Browser";
    }

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public RegisterResponse register(RegisterRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();

        log.info("[Auth] Register attempt: {}", MaskUtil.maskUsername(username));

        // Validate input trước khi check DB
        userValidator.validateUsername(username);
        userValidator.validateEmail(email);
        userValidator.validatePassword(request.getPassword());


        if (userRepositoryPort.existsByUsername(username)) {
            log.warn("[Auth] Username already exists: {}", MaskUtil.maskUsername(username));
            throw new UserException(ErrorCode.USR_002, ErrorCode.USR_002_MSG);
        }

        if (userRepositoryPort.existsByEmail(email)) {
            log.warn("[Auth] Email already exists: {}", MaskUtil.maskEmail(email));
            throw new UserException(ErrorCode.USR_003, ErrorCode.USR_003_MSG);
        }

        UUID userId = UUID.randomUUID();

        PasswordService.HashedPassword hashed =
                passwordService.hash(request.getPassword(), username, userId.toString());

        User user = mapper.toEntity(request);
        user.setId(userId);
        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());

        Role userRole = roleRepositoryPort.findByCode("USER")
                .orElseThrow(() -> new InfrastructureException(
                        ErrorCode.CMN_001, ErrorCode.CMN_001_MSG));
        user.setRoles(Set.of(userRole));

        User savedUser = userRepositoryPort.save(user);
        log.info("[Auth] Register successful: {}", MaskUtil.maskUsername(username));

        eventPublisher.publish(new UserRegisteredEvent(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail()
        ));

        RegisterResponse response = mapper.toResponse(savedUser);
        response.setMessage("Register successful");
        return response;
    }

}
