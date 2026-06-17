package com.booking.application.service.serviceimpl;

import com.booking.application.port.out.UserSessionRepositoryPort;
import com.booking.application.service.SessionService;
import com.booking.domain.model.User;
import com.booking.domain.model.UserSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * SessionServiceImpl = implement SessionService
 * Dùng UserSessionRepositoryPort (Output Port) thay vì
 * UserSessionJpaRepository trực tiếp → tuân thủ Clean Architecture
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SessionServiceImpl implements SessionService {

    private final UserSessionRepositoryPort sessionRepo;

    // ══════════════════════════════════════════════════════
    // LEGACY API
    // ══════════════════════════════════════════════════════

    @Override
    @Transactional
    public void revokeAllSessions(UUID userId) {
        int n = sessionRepo.invalidateAllActiveByUserId(
            userId, UserSession.REASON_ADMIN_KILL
        );
        log.info("[Session] Revoked {} session(s) for user {}", n, userId);
    }

    @Override
    @Transactional
    public void revokeSession(String jti) {
        invalidateByJti(jti, UserSession.REASON_LOGOUT);
    }

    // ══════════════════════════════════════════════════════
    // SINGLE SESSION API
    // ══════════════════════════════════════════════════════

    @Override
    @Transactional
    public int invalidateByJti(String jti, String reason) {
        if (jti == null || jti.isBlank()) {
            return 0;
        }
        int n = sessionRepo.invalidateByJti(jti, reason);
        if (n > 0) {
            log.info("[Session] Invalidated session jti: {} (reason: {})",
                jti, reason);
        }
        return n;
    }

    @Override
    @Transactional
    public int killOldSessions(UUID userId, String reason) {
        int killed = sessionRepo.invalidateAllActiveByUserId(userId, reason);
        if (killed > 0) {
            log.info("[Session] Killed {} old session(s) for user {} (reason: {})",
                killed, userId, reason);
        }
        return killed;
    }

    @Override
    @Transactional
    public UserSession createSession(User user, String jti, String authSource,
                                     String deviceInfo, String ipAddress,
                                     String userAgent, int ttlSeconds) {
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setSessionId(UUID.randomUUID().toString());
        session.setJti(jti);
        session.setAuthSource(authSource);
        session.setDeviceInfo(deviceInfo);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setIssuedAt(ZonedDateTime.now());
        session.setExpiresAt(ZonedDateTime.now().plusSeconds(ttlSeconds));
        session.setLastActiveAt(ZonedDateTime.now());

        UserSession saved = sessionRepo.save(session);
        log.info("[Session] Created new session for user {} (jti: {}, source: {}, expires: {})",
            user.getUsername(), jti, authSource, saved.getExpiresAt());
        return saved;
    }

    @Override
    @Transactional
    public Optional<UserSession> verifyActive(String jti) {
        Optional<UserSession> opt = sessionRepo.findByJti(jti);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        UserSession session = opt.get();
        if (!session.isActive()) {
            return Optional.empty();
        }

        // Touch last_active (best effort, không block)
        try {
            session.touch();
            sessionRepo.save(session);
        } catch (Exception e) {
            log.debug("[Session] Failed to touch lastActive for jti: {}", jti);
        }

        return Optional.of(session);
    }

    @Override
    @Transactional
    public int cleanupExpired() {
        int n = sessionRepo.cleanupExpired();
        if (n > 0) {
            log.info("[Session] Cleaned up {} expired session(s)", n);
        }
        return n;
    }
}
