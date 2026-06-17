package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.KcTokenRepositoryPort;
import com.booking.domain.model.KcToken;
import com.booking.infrastructure.persistence.entity.KcTokenEntity;
import com.booking.infrastructure.persistence.mapper.KcTokenMapper;
import com.booking.infrastructure.persistence.repository.KcTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class KcTokenRepositoryAdapter implements KcTokenRepositoryPort {

    private final KcTokenJpaRepository jpaRepository;
    private final KcTokenMapper mapper;

    @Override
    @Transactional
    public KcToken save(KcToken token) {
        KcTokenEntity entity = mapper.toEntity(token);
        KcTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<KcToken> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<KcToken> findByKcUserId(String kcUserId) {
        return jpaRepository.findByKcUserId(kcUserId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.findByUserId(userId).ifPresent(jpaRepository::delete);
    }

}
