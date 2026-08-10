package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.RoomTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoomTypeJpaRepository extends JpaRepository<RoomTypeEntity, UUID> {

    @Query("""
            SELECT rt FROM RoomTypeEntity rt
            WHERE (:hotelId IS NULL OR rt.hotel.id = :hotelId)
              AND (:active IS NULL OR rt.active = :active)
            """)
    Page<RoomTypeEntity> findForAdmin(
            @Param("hotelId") UUID hotelId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT rt FROM RoomTypeEntity rt
            WHERE (rt.hotel IS NULL OR rt.hotel.ownerUserId = :ownerUserId)
              AND (:active IS NULL OR rt.active = :active)
            """)
    Page<RoomTypeEntity> findForHost(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT rt FROM RoomTypeEntity rt
            WHERE UPPER(rt.code) = UPPER(:code)
              AND ((:hotelId IS NULL AND rt.hotel IS NULL) OR rt.hotel.id = :hotelId)
            """)
    Optional<RoomTypeEntity> findByScopeAndCode(
            @Param("hotelId") UUID hotelId,
            @Param("code") String code);

    @Query("""
            SELECT CASE WHEN COUNT(rt) > 0 THEN TRUE ELSE FALSE END
            FROM RoomTypeEntity rt
            WHERE UPPER(rt.code) = UPPER(:code)
              AND rt.active = TRUE
              AND (rt.hotel IS NULL OR rt.hotel.id = :hotelId)
            """)
    boolean existsActiveForHotelOrGlobal(
            @Param("hotelId") UUID hotelId,
            @Param("code") String code);
}
