package com.booking.infrastructure.persistence.repository;

import com.booking.domain.model.UserSession;
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
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Tìm session theo JTI (dùng để verify mỗi request)
     */
    Optional<UserSession> findByJti(String jti);

    /**
     * Lấy tất cả session còn active của user
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.invalidatedAt IS NULL")
    List<UserSession> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Invalidate tất cả session active của user
     * Dùng khi login mới (single session) hoặc admin kill
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.invalidatedAt = :now, " +
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
    @Query("UPDATE UserSession s SET s.invalidatedAt = :now, " +
           "s.invalidationReason = :reason " +
           "WHERE s.jti = :jti AND s.invalidatedAt IS NULL")
    int invalidateByJti(
        @Param("jti") String jti,
        @Param("reason") String reason,
        @Param("now") ZonedDateTime now
    );

    /**
     * Cleanup các session đã expired (chạy scheduled job)
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.invalidatedAt = :now, " +
           "s.invalidationReason = 'TOKEN_EXPIRED' " +
           "WHERE s.expiresAt < :now AND s.invalidatedAt IS NULL")
    int cleanupExpired(@Param("now") ZonedDateTime now);
}
