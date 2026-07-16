package com.booking.application.service.serviceimpl;

import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.service.JwtService;
import com.booking.application.service.TokenService;
import com.booking.infrastructure.external.cache.TokenCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * TokenServiceImpl
 *
 * Sau Phase A (Stateless):
 * - createToken() KHÔNG còn save vào DB. Login flow handle ở AuthServiceImpl.
 *   Method này deprecated, có thể xóa nếu không còn caller.
 * - validateToken(): verify ACCESS token:
 *   - Check JWT signature
 *   - Check Redis blacklist (fast-path)
 *   - Check DB blacklist (fallback nếu Redis miss)
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class TokenServiceImpl implements TokenService {

    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenBlacklistRepositoryPort blacklistRepositoryPort;
    private final TokenCacheService tokenCacheService;
    private final JwtService jwtService;

    @Override
    public String hashToken(String token) {
        return jwtService.hashToken(token);
    }

    @Override
    public boolean validateToken(String token) {
        // 1. Verify JWT signature + exp
        if (!jwtService.validateToken(token)) {
            log.warn("[Token] Invalid signature/expired");
            return false;
        }

        // 2. Extract jti
        String jti;
        try {
            jti = jwtService.extractJti(token);
        } catch (Exception e) {
            log.warn("[Token] Cannot extract jti: {}", e.getMessage());
            return false;
        }

        // 3. Check Redis blacklist (fast-path)
        if (tokenCacheService.isBlacklisted(jti)) {
            log.warn("[Token] Blacklisted (cache hit), jti: {}", jti);
            return false;
        }

        // 4. Fallback DB blacklist (Redis có thể miss sau restart)
        if (blacklistRepositoryPort.isBlacklisted(jti)) {
            log.warn("[Token] Blacklisted (DB hit), jti: {}", jti);
            return false;
        }

        return true;
    }

    /**
     * @deprecated
     * Login flow đã handle save token. Giữ stub để tránh break TokenService interface.
     * Sẽ xóa khi refactor TokenService interface (Bước 11).
     */
    @Override
    @Deprecated
    public String createToken(com.booking.domain.model.User user,
                              String ipAddress,
                              String userAgent) {
        log.warn("[Token] createToken() is deprecated — use AuthServiceImpl.login() instead");
        return jwtService.generateToken(user);
    }
}