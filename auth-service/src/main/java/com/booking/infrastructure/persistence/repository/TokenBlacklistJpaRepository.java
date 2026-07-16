package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.TokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TokenBlacklistJpaRepository
 *
 * jti là PK, dùng String làm ID type của JpaRepository.
 */
@Repository
public interface TokenBlacklistJpaRepository extends JpaRepository<TokenBlacklistEntity, String> {

 /**
  * Check 1 jti có trong blacklist không (PK lookup, O(1))
  * → Dùng trong TokenAuthFilter mỗi request (fallback khi Redis miss)
  */
 boolean existsByJti(String jti);

 /**
  * List tất cả jti đang blacklist của 1 user
  * → Dùng cho admin endpoint xem "user X có jti nào bị revoke"
  */
 List<TokenBlacklistEntity> findAllByUserIdOrderByBlacklistedAtDesc(UUID userId);

 /**
  * Cleanup job: xoá các entry đã hết hạn
  * → Schedule mỗi 24h
  */
 @Modifying
 @Transactional
 @Query("DELETE FROM TokenBlacklistEntity t WHERE t.expiresAt < :now")
 long deleteAllExpired(@Param("now") ZonedDateTime now);
}