package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * TokenJpaRepository
 * → Dùng TokenEntity (không phải Token domain)
 * → JPA chỉ biết Entity, không biết Domain
 *
 * V10 changes:
 * - Bỏ updateLastUsed (cột last_used_at đã DROP)
 * - Verify dùng jti là chính, token_hash chỉ dùng cho refresh flow
 */
@Repository
public interface TokenJpaRepository extends JpaRepository<TokenEntity, UUID> {

    /**
     * Tìm token theo hash + đang active
     * → Dùng cho REFRESH flow
     */
    Optional<TokenEntity> findByTokenHashAndIsActiveTrue(String tokenHash);

    /**
     * Tìm token theo jti
     * → Dùng cho admin revoke 1 session cụ thể
     */
    Optional<TokenEntity> findByJti(String jti);

    /**
     * Lấy refresh token đang active mới nhất của user
     */
    Optional<TokenEntity> findTopByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Deactivate 1 token theo jti
     * → Dùng cho logout, admin revoke specific
     *
     * @return số row updated (0 nếu jti không tồn tại hoặc đã inactive)
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE TokenEntity t
        SET t.isActive = false,
            t.deactivatedAt = :now,
            t.deactivationReason = :reason
        WHERE t.jti = :jti
          AND t.isActive = true
    """)
    int deactivateByJti(
            @Param("jti") String jti,
            @Param("now") ZonedDateTime now,
            @Param("reason") String reason
    );

    /**
     * Deactivate tất cả token đang active của 1 user
     * → Dùng cho NEW_LOGIN (single session) + admin revoke all
     */
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
            @Param("deactivatedAt") ZonedDateTime deactivatedAt,
            @Param("reason") String reason
    );
}