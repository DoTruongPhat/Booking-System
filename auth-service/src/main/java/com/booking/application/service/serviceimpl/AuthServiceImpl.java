package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.*;
import com.booking.application.port.out.DomainEventPublisher;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.service.*;
import com.booking.application.validator.UserValidator;
import com.booking.domain.event.PasswordResetRequestedEvent;
import com.booking.domain.event.UserRegisteredEvent;
import com.booking.domain.exception.*;
import com.booking.domain.model.Role;
import com.booking.domain.model.User;
import com.booking.presentation.mapper.UserMapper;
import com.booking.presentation.response.VerifyUserResponse;
import com.booking.shared.util.MaskUtil;
import com.booking.presentation.request.*;
import com.booking.presentation.response.LoginResponse;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.infrastructure.external.keycloak.KeycloakGateway;
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
        RegisterUseCase,
        LogoutUseCase,
        ForgotPasswordUseCase,
        ResetPasswordUseCase,
        Verify2faUseCase,
        VerifyUserUseCase{

    private final UserRepositoryPort userRepositoryPort;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final KeycloakGateway keycloakGateway;
    private final TokenService tokenService;
    private final TokenCacheService tokenCacheService;
    private final PasswordService passwordService;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserMapper mapper;
    private final TwoFactorService twoFactorService;
    private final SystemParamService systemParamService;

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

        // Bước 3: Xác thực với Keycloak
        boolean kcOk = keycloakGateway.authenticate(
                username, request.getPassword());

        if (!kcOk) {
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

        // Bước 5: Vô hiệu hóa tất cả token cũ
        int deactivated = tokenRepositoryPort
                .deactivateAllByUserId(userWithRoles.getId(), "NEW_LOGIN");
        log.info("[Auth] Deactivated {} old tokens for user {}",
                deactivated, MaskUtil.maskUsername(username));



        // Bước 6: Tạo token mới
        String rawToken = tokenService.createToken(userWithRoles, ipAddress, userAgent);
        log.info("[Auth] Login successful: {}", MaskUtil.maskUsername(username));

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

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public void logout(String rawToken) {
        log.info("[Auth] Logout request");

        String tokenHash = tokenService.hashToken(rawToken);

        tokenRepositoryPort.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.deactivate("LOGOUT");
                    tokenRepositoryPort.save(token);
                    tokenCacheService.deleteToken(
                            token.getUser().getId().toString());
                    log.info("[Auth] Logout successful for user {}",
                            MaskUtil.maskUsername(token.getUser().getUsername()));
                });
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

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        log.info("[Auth] Forgot password request: {}", MaskUtil.maskEmail(email));

        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        String resetToken = UUID.randomUUID().toString();
        tokenCacheService.saveResetToken(resetToken, user.getId().toString());

        eventPublisher.publish(new PasswordResetRequestedEvent(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                resetToken
        ));
        log.info("[Auth] Reset token sent to: {}", MaskUtil.maskEmail(email));
    }

    @Override
    @Transactional(rollbackFor = DomainException.class)
    public void resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        log.info("[Auth] Reset password request");

        String userId = tokenCacheService.getResetToken(token);
        if (userId == null) {
            throw new TokenException(ErrorCode.TKN_001, ErrorCode.TKN_001_MSG);
        }

        User user = userRepositoryPort.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        PasswordService.HashedPassword hashed =
                passwordService.hash(
                        request.getNewPassword(),
                        user.getUsername(),
                        user.getPasswordSalt());

        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepositoryPort.save(user);
        tokenCacheService.deleteResetToken(token);

        log.info("[Auth] Password reset successful for user: {}",
                MaskUtil.maskUsername(user.getUsername()));
    }

    @Override
    public LoginResponse verify2fa(TwoFactorRequest request,
                                   String ipAddress,
                                   String userAgent) {
        log.info("[Auth] 2FA verify request");

        String userId = tokenCacheService.getMfaSession(request.getSessionToken());
        if (userId == null) {
            throw new TokenException(ErrorCode.TKN_001, ErrorCode.TKN_001_MSG);
        }

        User user = userRepositoryPort
                .findByIdWithRoles(UUID.fromString(userId))
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        if (!twoFactorService.verifyOtp(user.getTotpSecret(), request.getOtp())) {
            throw new AuthException(ErrorCode.AUTH_001, "Invalid OTP");
        }

        tokenCacheService.deleteMfaSession(request.getSessionToken());
        tokenRepositoryPort.deactivateAllByUserId(user.getId(), "NEW_LOGIN");

        String jwt = tokenService.createToken(user, ipAddress, userAgent);
        log.info("[Auth] 2FA verify successful: {}",
                MaskUtil.maskUsername(user.getUsername()));

        return LoginResponse.builder()
                .token(jwt)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(r -> r.getCode())
                        .toList())
                .timezone(user.getTimezone())
                .twoFactorRequired(false)
                .build();
    }

    @Override
    public VerifyUserResponse verifyUser(VerifyUserRequest request) {
        log.info("[Auth] Verify user request: {}",
                MaskUtil.maskUsername(request.getUsername()));

        // Bước 1: Tìm user
        User user = userRepositoryPort
                .findByUsername(request.getUsername())
                .orElse(null);

        // Không tìm thấy → valid = false
        if (user == null) {
            log.warn("[Auth] User not found for verify: {}",
                    MaskUtil.maskUsername(request.getUsername()));
            VerifyUserResponse response = new VerifyUserResponse();
            response.setValid(false);
            return response;
        }

        // Bước 2: Verify password
        boolean isValid = passwordService.verify(
                request.getPassword(),
                user.getPasswordHash(),
                user.getPasswordSalt(),
                user.getUsername()
        );

        // Sai password → valid = false
        if (!isValid) {
            log.warn("[Auth] Verify failed for: {}",
                    MaskUtil.maskUsername(request.getUsername()));
            VerifyUserResponse response = new VerifyUserResponse();
            response.setValid(false);
            return response;
        }

        // Bước 3: Build response
        VerifyUserResponse response = new VerifyUserResponse();
        response.setValid(true);
        response.setUserId(user.getId().toString());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRoles(
                user.getRoles() == null ? List.of() :
                        user.getRoles().stream()
                                .map(r -> r.getCode())
                                .toList()
        );

        log.info("[Auth] Verify successful for: {}",
                MaskUtil.maskUsername(request.getUsername()));
        return response;

    }
}
