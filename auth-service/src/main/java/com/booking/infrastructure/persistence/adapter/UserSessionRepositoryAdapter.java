package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.UserSessionRepositoryPort;
import com.booking.domain.model.UserSession;
import com.booking.infrastructure.persistence.entity.UserSessionEntity;
import com.booking.infrastructure.persistence.mapper.UserSessionMapper;
import com.booking.infrastructure.persistence.repository.UserSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * UserSessionRepositoryAdapter = implement UserSessionRepositoryPort
 * Bridge giữa domain UserSession và JPA UserSessionEntity
 */
@Repository
@RequiredArgsConstructor
public class UserSessionRepositoryAdapter implements UserSessionRepositoryPort {

    private final UserSessionJpaRepository jpaRepository;
    private final UserSessionMapper mapper;

    @Override
    public Optional<UserSession> findByJti(String jti) {
        return jpaRepository.findByJti(jti)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public UserSession save(UserSession session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID());
        }
        UserSessionEntity entity = mapper.toEntity(session);
        UserSessionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public int invalidateAllActiveByUserId(UUID userId, String reason) {
        return jpaRepository.invalidateAllActiveByUserId(
            userId, reason, ZonedDateTime.now()
        );
    }

    @Override
    @Transactional
    public int invalidateByJti(String jti, String reason) {
        return jpaRepository.invalidateByJti(
            jti, reason, ZonedDateTime.now()
        );
    }

    @Override
    @Transactional
    public int cleanupExpired() {
        return jpaRepository.cleanupExpired(ZonedDateTime.now());
    }

}
