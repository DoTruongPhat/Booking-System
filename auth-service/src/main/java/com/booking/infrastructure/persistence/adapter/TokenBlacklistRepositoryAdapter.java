package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.TokenBlacklistRepositoryPort;
import com.booking.domain.model.TokenBlacklist;
import com.booking.infrastructure.persistence.entity.TokenBlacklistEntity;
import com.booking.infrastructure.persistence.mapper.TokenBlacklistMapper;
import com.booking.infrastructure.persistence.repository.TokenBlacklistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TokenBlacklistRepositoryAdapter implements TokenBlacklistRepositoryPort {

 private final TokenBlacklistJpaRepository jpaRepository;
 private final TokenBlacklistMapper mapper;

 @Override
 public boolean isBlacklisted(String jti) {
  return jpaRepository.existsByJti(jti);
 }

 @Override
 public void blacklist(String jti, UUID userId, String reason, ZonedDateTime expiresAt) {
  TokenBlacklistEntity entity = new TokenBlacklistEntity(jti, userId, expiresAt, reason);
  jpaRepository.save(entity);
 }

 @Override
 public Optional<TokenBlacklist> findByJti(String jti) {
  return jpaRepository.findById(jti)
          .map(mapper::toDomain);
 }

 @Override
 public List<TokenBlacklist> findAllByUserId(UUID userId) {
  return jpaRepository.findAllByUserIdOrderByBlacklistedAtDesc(userId)
          .stream()
          .map(mapper::toDomain)
          .toList();
 }

 @Override
 public long deleteExpired() {
  return jpaRepository.deleteAllExpired(ZonedDateTime.now());
 }
}