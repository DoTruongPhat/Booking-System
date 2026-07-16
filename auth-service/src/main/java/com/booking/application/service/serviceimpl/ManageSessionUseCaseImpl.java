package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.ManageSessionUseCase;
import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
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
import java.util.UUID;

/**
 * ManageSessionUseCaseImpl (Phase A.4 — Stateless)
 *
 * Dùng cho ADMIN endpoints:
 *   - revokeSession(jti):       kick 1 session cụ thể (DELETE /admin/sessions/{jti})
 *   - revokeAllSessions(userId): kick toàn bộ session của 1 user (DELETE /admin/users/{userId}/revoke)
 *
 * Mỗi revoke gồm 3 việc:
 *   1. Deactivate row trong auth.tokens (is_active = false, reason = ADMIN_REVOKE)
 *   2. Insert jti vào auth.tokens_blacklist
 *   3. Cache blacklist:{jti} trong Redis (fast-path cho TokenAuthFilter)
 *
 * Cách A — access JWT cũ bị reject ngay nhờ blacklist check ở TokenAuthFilter.
 * Refresh token đã deactivate → user phải re-login.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ManageSessionUseCaseImpl implements ManageSessionUseCase {

    private final TokenRepositoryPort tokenRepositoryPort;
    private final TokenBlacklistRepositoryPort blacklistRepositoryPort;
    private final TokenCacheService tokenCacheService;

    @Override
    @Transactional
    public void revokeSession(String jti) {
        log.info("[ManageSession] Revoke session jti={}", jti);

        if (jti == null || jti.isBlank()) {
            log.warn("[ManageSession] Empty jti, skip");
            return;
        }

        Optional<Token> tokenOpt = tokenRepositoryPort.findByJti(jti);
        if (tokenOpt.isEmpty()) {
            log.warn("[ManageSession] Token row not found for jti={} (maybe already revoked)", jti);
            return;
        }
        Token token = tokenOpt.get();

        boolean deactivated = tokenRepositoryPort.deactivateByJti(
                jti, DeactivationReason.ADMIN_REVOKE.name());
        log.debug("[ManageSession] Deactivated by jti={}: {}", jti, deactivated);

        ZonedDateTime expiresAt = token.getExpiresAt() != null
                ? token.getExpiresAt()
                : ZonedDateTime.now().plusHours(1);

        blacklistRepositoryPort.blacklist(
                jti,
                token.getUser().getId(),
                BlacklistReason.ADMIN_REVOKE.name(),
                expiresAt);

        tokenCacheService.blacklist(jti, expiresAt);

        log.info("[ManageSession] Revoked session jti={} for user={}",
                jti, token.getUser().getId());
    }

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        log.info("[ManageSession] Revoke all sessions for user={}", userId);

        if (userId == null) {
            log.warn("[ManageSession] Null userId, skip");
            return;
        }

        // Lấy token active hiện tại (Single Session: tối đa 1)
        Optional<Token> activeTokenOpt = tokenRepositoryPort.findActiveTokenByUserId(userId);

        // Deactivate all
        int killed = tokenRepositoryPort.deactivateAllByUserId(
                userId, DeactivationReason.ADMIN_REVOKE.name());
        log.info("[ManageSession] Deactivated {} token(s) for user={}", killed, userId);

        // Blacklist jti nếu có active token
        if (activeTokenOpt.isPresent()) {
            Token token = activeTokenOpt.get();
            String jti = token.getJti();
            ZonedDateTime expiresAt = token.getExpiresAt() != null
                    ? token.getExpiresAt()
                    : ZonedDateTime.now().plusHours(1);

            blacklistRepositoryPort.blacklist(
                    jti,
                    userId,
                    BlacklistReason.ADMIN_REVOKE.name(),
                    expiresAt);

            tokenCacheService.blacklist(jti, expiresAt);

            log.info("[ManageSession] Blacklisted jti={} for user={}", jti, userId);
        }
    }
}