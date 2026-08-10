package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BookingJpaRepository extends JpaRepository<BookingEntity, UUID>,
                                        JpaSpecificationExecutor<BookingEntity> {

    @Query("SELECT b FROM BookingEntity b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.userId = :userId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByUserIdAndStatus(@Param("userId") UUID userId,
                                               @Param("status") String status,
                                               Pageable pageable);

    @Query("SELECT b FROM BookingEntity b WHERE b.hotel.id = :hotelId ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByHotelId(@Param("hotelId") UUID hotelId, Pageable pageable);

    boolean existsByHotelId(UUID hotelId);

    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.hotel.id = :hotelId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByHotelIdAndStatus(@Param("hotelId") UUID hotelId,
                                               @Param("status") String status,
                                               Pageable pageable);

    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.hotel.ownerUserId = :ownerUserId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "ORDER BY b.createdAt DESC")
    Page<BookingEntity> findByOwnerUserIdAndStatus(@Param("ownerUserId") UUID ownerUserId,
                                                   @Param("status") String status,
                                                   Pageable pageable);

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.hotel.ownerUserId = :ownerUserId")
    long countByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.hotel.ownerUserId = :ownerUserId AND b.status = :status")
    long countByOwnerUserIdAndStatus(@Param("ownerUserId") UUID ownerUserId,
                                     @Param("status") String status);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM BookingEntity b " +
            "WHERE b.hotel.ownerUserId = :ownerUserId AND b.paymentStatus = 'PAID'")
    BigDecimal sumPaidRevenueByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT COUNT(b) FROM BookingEntity b " +
            "WHERE b.hotel.ownerUserId = :ownerUserId " +
            "AND b.checkInDate BETWEEN :startDate AND :endDate " +
            "AND b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')")
    long countUpcomingCheckIns(@Param("ownerUserId") UUID ownerUserId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM BookingEntity b " +
            "WHERE (:status IS NULL OR b.status = :status) " +
            "ORDER BY b.createdAt DESC")
    Page<BookingEntity> findAllByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Check overlapping bookings for same user + room (BR-BOOK-018).
     * Two ranges overlap when: existingCheckIn < newCheckOut AND existingCheckOut > newCheckIn
     */
    @Query("SELECT COUNT(b) > 0 FROM BookingEntity b " +
            "WHERE b.userId = :userId " +
            "AND b.room.id = :roomId " +
            "AND b.status NOT IN ('CANCELLED', 'NO_SHOW') " +
            "AND b.checkInDate < :checkOut " +
            "AND b.checkOutDate > :checkIn")
    boolean existsOverlapping(
            @Param("userId") UUID userId,
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    @Query("SELECT COUNT(b) FROM BookingEntity b WHERE b.bookingCode LIKE :prefix%")
    long countByBookingCodePrefix(@Param("prefix") String prefix);

    /** BR-CRON-001: PENDING bookings created before cutoff */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.status = 'PENDING' AND b.createdAt < :cutoff")
    List<BookingEntity> findExpiredPending(@Param("cutoff") Instant cutoff);

    /** BR-CRON-002: CONFIRMED bookings with check-in date before cutoff (24h+ ago) */
    @Query("SELECT b FROM BookingEntity b " +
            "WHERE b.status = 'CONFIRMED' AND b.checkInDate < :checkInCutoff")
    List<BookingEntity> findNoShowCandidates(@Param("checkInCutoff") LocalDate checkInCutoff);


    @Query("SELECT COUNT(DISTINCT b.id) " +
            "FROM BookingEntity b " +
            "WHERE b.room.id = :roomId " +
            "AND b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN') " +
            "AND b.checkOutDate >= :today")
    long countActiveBookingsForRoom(@Param("roomId") UUID roomId,
                                    @Param("today") LocalDate today);

    @Query("SELECT b FROM BookingEntity b WHERE b.status = 'PENDING' " +
            "AND b.paymentStatus = 'UNPAID' " +
            "AND b.checkInDate <= :cutoffDate")
    List<BookingEntity> findPendingBeforeDate(@Param("cutoffDate") LocalDate cutoffDate);
}
