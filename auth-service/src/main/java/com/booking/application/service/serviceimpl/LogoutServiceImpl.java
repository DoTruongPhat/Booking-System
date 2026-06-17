package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.LogoutUseCase;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.SessionService;
import com.booking.application.service.TokenService;
import com.booking.domain.model.UserSession;
import com.booking.infrastructure.external.cache.TokenCacheService;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LogoutServiceImpl
 *
 * LUỒNG:
 *   1. Extract jti từ JWT
 *   2. Invalidate session trong DB (single session tracking)
 *   3. Deactivate token cũ (legacy backward compat)
 *
 * TÁCH RIÊNG vì:
 *   - Logout là 1 use case độc lập
 *   - AuthServiceImpl đang quá nhiều logic
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class LogoutServiceImpl implements LogoutUseCase {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenService tokenService;
    private final TokenCacheService tokenCacheService;

    @Override
    @Transactional
    public void logout(String rawToken) {
        log.info("[Logout] Request");

        if (rawToken == null || rawToken.isBlank()) {
            log.warn("[Logout] Empty token");
            return;
        }

        // Bước 1: Invalidate session qua jti
        try {
            String jti = jwtService.extractJti(rawToken);
            if (jti != null && !jti.isBlank()) {
                sessionService.invalidateByJti(jti, UserSession.REASON_LOGOUT);
            }
        } catch (Exception e) {
            log.warn("[Logout] Cannot extract jti: {}", e.getMessage());
        }

        // Bước 2: Legacy - deactivate token trong tokens table
        try {
            String tokenHash = tokenService.hashToken(rawToken);
            tokenRepositoryPort.findByTokenHash(tokenHash).ifPresent(token -> {
                token.deactivate("LOGOUT");
                tokenRepositoryPort.save(token);
                tokenCacheService.deleteToken(token.getUser().getId().toString());
                log.info("[Logout] Success for {}",
                    MaskUtil.maskUsername(token.getUser().getUsername()));
            });
        } catch (Exception e) {
            log.error("[Logout] Legacy cleanup failed: {}", e.getMessage());
        }
    }
}
