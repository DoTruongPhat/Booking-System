package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.RefreshTokenUseCase;
import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.domain.enums.BlacklistReason;
import com.booking.domain.enums.DeactivationReason;
import com.booking.domain.exception.AuthException;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.model.Role;
import com.booking.domain.model.Token;
import com.booking.domain.model.User;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.presentation.response.LoginResponse;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshTokenServiceImpl (Phase A.4 — rotate jti)
 *
 * Bảo mật:
 *  - Verify signature + exp
 *  - Check blacklist (Redis → DB fallback)
 *  - Hash refresh + match với row active trong auth.tokens
 *  - Verify jti từ JWT match với jti DB (sanity check chống replay)
 *  - Rotate jti — token cũ deactivate + blacklist, token mới được issue
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class RefreshTokenServiceImpl implements RefreshTokenUseCase {

    private final JwtService jwtService;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenBlacklistRepositoryPort blacklistRepositoryPort;
    private final TokenCacheService tokenCacheService;
    private final UserRepositoryPort userRepositoryPort;

    private static final int REFRESH_TTL_DAYS = 7;

    @Override
    @Transactional(rollbackFor = AuthException.class)
    public LoginResponse refresh(String refreshToken, String ipAddress, String userAgent) {

        // 1. Verify signature + exp
        if (!jwtService.validateToken(refreshToken)) {
            log.warn("[Refresh] Invalid signature/expired");
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }

        // 2. Verify đây là refresh token, không phải access
        if (!jwtService.isRefreshToken(refreshToken)) {
            log.warn("[Refresh] Not a refresh token");
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }

        // 3. Extract jti + username
        String oldJti = jwtService.extractJti(refreshToken);
        String username = jwtService.extractUsername(refreshToken);

        log.info("[Refresh] Attempt for user={} (jti={})",
                MaskUtil.maskUsername(username), oldJti);

        // 4. Check blacklist (Redis fast-path)
        if (tokenCacheService.isBlacklisted(oldJti)) {
            log.warn("[Refresh] Blacklisted (cache), jti={}", oldJti);
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }
        // 5. Fallback DB blacklist
        if (blacklistRepositoryPort.isBlacklisted(oldJti)) {
            log.warn("[Refresh] Blacklisted (DB), jti={}", oldJti);
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }

        // 6. Hash refresh + find row active trong auth.tokens
        String oldHash = jwtService.hashToken(refreshToken);
        Optional<Token> rowOpt = tokenRepositoryPort.findByTokenHash(oldHash);
        if (rowOpt.isEmpty()) {
            log.warn("[Refresh] Token row not found (deactivated or never existed), jti={}", oldJti);
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }
        Token oldRow = rowOpt.get();

        // 7. Sanity check: jti from JWT must equal jti in DB row
        if (!oldJti.equals(oldRow.getJti())) {
            log.warn("[Refresh] jti mismatch — JWT={} DB={}", oldJti, oldRow.getJti());
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }

        // 8. Verify expires_at > now (defense in depth, JWT exp đã check ở bước 1)
        if (oldRow.getExpiresAt() != null && oldRow.getExpiresAt().isBefore(ZonedDateTime.now())) {
            log.warn("[Refresh] DB row expired, jti={}", oldJti);
            throw new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG);
        }

        // 9. Load user with roles (cho permissions/roles claims mới nhất)
        User userWithRoles = userRepositoryPort
                .findByIdWithRoles(oldRow.getUser().getId())
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_003, ErrorCode.AUTH_003_MSG));

        // 10. ROTATE — generate new jti + tokens
        String newJti = UUID.randomUUID().toString();
        String newAccessToken = jwtService.generateToken(userWithRoles, newJti);
        String newRefreshToken = jwtService.generateRefreshToken(userWithRoles, newJti);
        String newHash = jwtService.hashToken(newRefreshToken);

        // 11. Deactivate row cũ
        tokenRepositoryPort.deactivateByJti(oldJti, DeactivationReason.NEW_LOGIN.name());

        // 12. Blacklist jti cũ (DB + Redis) — chống replay
        ZonedDateTime oldExpires = oldRow.getExpiresAt() != null
                ? oldRow.getExpiresAt()
                : ZonedDateTime.now().plusDays(REFRESH_TTL_DAYS);

        blacklistRepositoryPort.blacklist(
                oldJti,
                userWithRoles.getId(),
                BlacklistReason.NEW_LOGIN.name(),
                oldExpires);

        tokenCacheService.blacklist(oldJti, oldExpires);

        // 13. Insert row mới với refresh hash + jti mới
        Token newRow = new Token();
        // KHÔNG setId() — để JPA tự gen (đã fix bug optimistic locking)
        newRow.setUser(userWithRoles);
        newRow.setTokenHash(newHash);
        newRow.setJti(newJti);
        newRow.setActive(true);
        newRow.setIpAddress(ipAddress);
        newRow.setUserAgent(userAgent);
        newRow.setCreatedAt(ZonedDateTime.now());
        newRow.setExpiresAt(ZonedDateTime.now().plusDays(REFRESH_TTL_DAYS));
        tokenRepositoryPort.save(newRow);

        log.info("[Refresh] Rotated jti {} → {} for user={}",
                oldJti, newJti, MaskUtil.maskUsername(username));

        // 14. Trả response
        return LoginResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(userWithRoles.getUsername())
                .email(userWithRoles.getEmail())
                .roles(userWithRoles.getRoles().stream()
                        .map(Role::getCode)
                        .toList())
                .timezone(userWithRoles.getTimezone())
                .twoFactorRequired(false)
                .build();
    }
}