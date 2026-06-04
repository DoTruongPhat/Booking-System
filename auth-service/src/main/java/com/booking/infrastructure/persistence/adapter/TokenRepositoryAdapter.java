package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.TokenRepositoryPort;

import com.booking.domain.model.Token;
import com.booking.infrastructure.persistence.mapper.TokenEntityMapper;
import com.booking.infrastructure.persistence.repository.TokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TokenRepositoryAdapter implements TokenRepositoryPort {

    private final TokenJpaRepository jpaRepository;

    private final TokenEntityMapper tokenEntityMapper;

    @Override
    public Token save(Token token) {
        return tokenEntityMapper.toDomain(
                jpaRepository.save(tokenEntityMapper.toEntity(token))
        );
    }

    @Override
    public Optional<Token> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHashAndIsActiveTrue(tokenHash)
                .map(tokenEntityMapper::toDomain);
    }

    @Override
    public int deactivateAllByUserId(UUID userId, String reason) {
        return jpaRepository.deactivateAllByUserId
                (userId,
                        ZonedDateTime.now(),
                        reason);
    }

    @Override
    public void updateLastUsed(String tokenHash) {
        jpaRepository.updateLastUsed(tokenHash, ZonedDateTime.now());
    }

    @Override
    public Optional<Token> findActiveTokenByUserId(UUID userId) {
        return jpaRepository.findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)
                .map(tokenEntityMapper::toDomain);
    }

    @Override
    public Optional<Token> findByJti(String jti) {
        // Delegate xuống JpaRepository
        // → Spring Data tự generate SQL
        // → Không cần viết query thủ công
        return jpaRepository.findByJti(jti)
                .map(tokenEntityMapper::toDomain);
    }
}
