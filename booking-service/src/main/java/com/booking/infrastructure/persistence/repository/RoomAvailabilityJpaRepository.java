package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.RoomAvailabilityEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomAvailabilityJpaRepository extends JpaRepository<RoomAvailabilityEntity, UUID> {

    @Query("SELECT ra FROM RoomAvailabilityEntity ra " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate " +
            "ORDER BY ra.date ASC")
    List<RoomAvailabilityEntity> findByRoomIdAndDateRange(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MIN(ra.availableCount) FROM RoomAvailabilityEntity ra " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate")
    Integer findMinAvailableCount(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ra FROM RoomAvailabilityEntity ra " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate " +
            "ORDER BY ra.date ASC")
    List<RoomAvailabilityEntity> findByRoomIdAndDateRangeForUpdate(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE RoomAvailabilityEntity ra " +
            "SET ra.status = 'BLOCKED', ra.availableCount = 0, ra.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate")
    void blockDates(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("UPDATE RoomAvailabilityEntity ra " +
            "SET ra.availableCount = ra.availableCount - :numRooms, ra.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate")
    void decrementAvailability(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("numRooms") int numRooms);

    @Modifying
    @Query("UPDATE RoomAvailabilityEntity ra " +
            "SET ra.availableCount = ra.availableCount + :numRooms, ra.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ra.room.id = :roomId " +
            "AND ra.date >= :startDate AND ra.date < :endDate")
    void incrementAvailability(
            @Param("roomId") UUID roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("numRooms") int numRooms);
}
