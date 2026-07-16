package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.LogoutUseCase;
import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.domain.enums.BlacklistReason;
import com.booking.domain.enums.DeactivationReason;
import com.booking.domain.model.Token;
import com.booking.infrastructure.external.cache.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * LogoutServiceImpl (Phase A.4 — Stateless)
 *
 * Khi user logout:
 *  1. Extract jti từ access token (rawToken)
 *  2. Blacklist jti access (DB + Redis) → access không dùng được dù chưa exp
 *  3. Deactivate refresh token trong auth.tokens (jti match cùng row, hoặc deactivate all by userId)
 *
 * Note: trong Phase A hiện tại, login đang lưu jti của access vào auth.tokens
 *   (TODO Bước 11 sẽ tách jti access/refresh). Vì vậy chỉ cần deactivate 1 jti là đủ.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class LogoutServiceImpl implements LogoutUseCase {

    private final JwtService jwtService;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenBlacklistRepositoryPort blacklistRepositoryPort;
    private final TokenCacheService tokenCacheService;

    @Override
    @Transactional
    public void logout(String rawToken) {
        log.info("[Logout] Request");

        if (rawToken == null || rawToken.isBlank()) {
            log.warn("[Logout] Empty token");
            return;
        }

        String jti;
        try {
            jti = jwtService.extractJti(rawToken);
        } catch (Exception e) {
            log.warn("[Logout] Cannot extract jti: {}", e.getMessage());
            return;
        }

        if (jti == null || jti.isBlank()) {
            log.warn("[Logout] Empty jti, skip");
            return;
        }

        // 1. Tìm token row trong auth.tokens (để lấy userId + expiresAt)
        Optional<Token> tokenOpt = tokenRepositoryPort.findByJti(jti);
        if (tokenOpt.isEmpty()) {
            log.warn("[Logout] Token row not found for jti: {} (maybe already revoked)", jti);
            return;
        }
        Token token = tokenOpt.get();

        // 2. Deactivate row trong auth.tokens
        boolean deactivated = tokenRepositoryPort.deactivateByJti(
                jti, DeactivationReason.LOGOUT.name());
        log.debug("[Logout] Deactivated by jti={}: {}", jti, deactivated);

        // 3. Blacklist jti — DB
        blacklistRepositoryPort.blacklist(
                jti,
                token.getUser().getId(),
                BlacklistReason.LOGOUT.name(),
                token.getExpiresAt() != null
                        ? token.getExpiresAt()
                        : ZonedDateTime.now().plusHours(1)  // safety fallback
        );

        // 4. Blacklist jti — Redis cache
        tokenCacheService.blacklist(
                jti,
                token.getExpiresAt() != null
                        ? token.getExpiresAt()
                        : ZonedDateTime.now().plusHours(1)
        );

        log.info("[Logout] Success, jti: {}", jti);
    }
}