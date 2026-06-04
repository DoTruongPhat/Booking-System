package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * TokenJpaRepository
 * → Dùng TokenEntity (không phải Token domain)
 * → JPA chỉ biết Entity, không biết Domain
 */
@Repository
public interface TokenJpaRepository
        extends JpaRepository<TokenEntity, UUID> {

    Optional<TokenEntity> findByTokenHashAndIsActiveTrue(String tokenHash);

    Optional<TokenEntity> findByJti(String jti);

    Optional<TokenEntity> findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE TokenEntity t
        SET t.isActive = false,
            t.deactivatedAt = :deactivatedAt,
            t.deactivationReason = :reason
        WHERE t.user.id = :userId
          AND t.isActive = true
    """)
    int deactivateAllByUserId(
            @Param("userId") UUID userId,
            @Param("deactivatedAt") java.time.ZonedDateTime deactivatedAt,
            @Param("reason") String reason
    );

    @Modifying
    @Transactional
    @Query("UPDATE TokenEntity t SET t.lastUsedAt = :now WHERE t.tokenHash = :hash")
    void updateLastUsed(
            @Param("hash") String tokenHash,
            @Param("now") java.time.ZonedDateTime now
    );
}