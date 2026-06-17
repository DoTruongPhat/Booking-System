package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, UUID> {

    /**
     * Tìm session theo JTI (dùng để verify mỗi request)
     */
    Optional<UserSessionEntity> findByJti(String jti);

    /**
     * Lấy tất cả session còn active của user
     */
    @Query("SELECT s FROM UserSessionEntity s WHERE s.userId = :userId AND s.invalidatedAt IS NULL")
    List<UserSessionEntity> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Invalidate tất cả session active của user
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.invalidatedAt = :now, " +
           "s.invalidationReason = :reason " +
           "WHERE s.userId = :userId AND s.invalidatedAt IS NULL")
    int invalidateAllActiveByUserId(
        @Param("userId") UUID userId,
        @Param("reason") String reason,
        @Param("now") ZonedDateTime now
    );

    /**
     * Invalidate 1 session theo jti
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.invalidatedAt = :now, " +
           "s.invalidationReason = :reason " +
           "WHERE s.jti = :jti AND s.invalidatedAt IS NULL")
    int invalidateByJti(
        @Param("jti") String jti,
        @Param("reason") String reason,
        @Param("now") ZonedDateTime now
    );

    /**
     * Cleanup expired sessions
     */
    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.invalidatedAt = :now, " +
           "s.invalidationReason = 'TOKEN_EXPIRED' " +
           "WHERE s.expiresAt < :now AND s.invalidatedAt IS NULL")
    int cleanupExpired(@Param("now") ZonedDateTime now);
}
