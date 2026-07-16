package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, UUID> {

    Page<RoomEntity> findByHotelId(UUID hotelId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.hotel.ownerUserId = :ownerUserId")
    long countByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT COALESCE(SUM(r.totalRooms), 0) FROM RoomEntity r WHERE r.hotel.ownerUserId = :ownerUserId")
    long sumTotalRoomsByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.hotel.ownerUserId = :ownerUserId AND r.status = :status")
    long countByOwnerUserIdAndStatus(@Param("ownerUserId") UUID ownerUserId,
                                     @Param("status") String status);
}
