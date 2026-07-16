package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.UserKcLinkRepositoryPort;
import com.booking.domain.model.UserKcLink;
import com.booking.infrastructure.persistence.mapper.UserKcLinkMapper;
import com.booking.infrastructure.persistence.repository.UserKcLinkJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserKcLinkRepositoryAdapter implements UserKcLinkRepositoryPort {

    private final UserKcLinkJpaRepository jpaRepository;
    private final UserKcLinkMapper mapper;

    @Override
    public Optional<UserKcLink> findByKcUserId(String kcUserId) {
        return jpaRepository.findByKcUserId(kcUserId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<UserKcLink> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public UserKcLink save(UserKcLink link) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(link)));
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}