package com.booking.application.service;

import com.booking.domain.enums.BookingStatus;
import com.booking.infrastructure.persistence.entity.BookingEntity;
import com.booking.infrastructure.persistence.repository.BookingJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {

    private final BookingJpaRepository bookingRepository;

    /**
     * Mỗi giờ check:
     * Booking PENDING + checkInDate - now <= 2 ngày → auto CANCEL
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    @Transactional
    public void cancelUnpaidBookings() {
        LocalDate cutoffDate = LocalDate.now().plusDays(2);

        List<BookingEntity> unpaidBookings = bookingRepository.findPendingBeforeDate(cutoffDate);

        if (unpaidBookings.isEmpty()) return;

        log.info("Found {} unpaid bookings to cancel (checkIn <= {})", unpaidBookings.size(), cutoffDate);

        for (BookingEntity booking : unpaidBookings) {
            try {
                booking.setStatus(BookingStatus.CANCELLED.name());
                booking.setCancellationReason("Auto-cancelled: not paid within 2 days before check-in");
                booking.setCancelledAt(Instant.now());
                booking.setCancelledBy("SYSTEM");
                booking.setUpdatedAt(Instant.now());
                bookingRepository.save(booking);

                log.info("Booking auto-cancelled: code={}, checkIn={}, reason=unpaid",
                        booking.getBookingCode(), booking.getCheckInDate());

            } catch (Exception e) {
                log.error("Failed to cancel booking {}: {}", booking.getBookingCode(), e.getMessage());
            }
        }
    }
}