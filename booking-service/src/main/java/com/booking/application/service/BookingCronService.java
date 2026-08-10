package com.booking.application.service;

import com.booking.application.port.in.CancelBookingUseCase;
import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.domain.enums.CancelledBy;
import com.booking.domain.model.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCronService {

    private final BookingRepositoryPort bookingRepository;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final PaymentDeadlinePolicy paymentDeadlinePolicy;

    @Value("${app.booking.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;

    /**
     * BR-CRON-001, BR-BOOK-017, BR-STATE-010: PENDING bookings older than the configured
     * payment window are auto-cancelled, restoring availability.
     * Runs every 1 minute.
     */
    @Scheduled(fixedRate = 60_000)
    public void cancelExpiredPendingBookings() {
        Instant cutoff = Instant.now().minus(pendingTimeoutMinutes, ChronoUnit.MINUTES);
        Instant now = Instant.now();
        List<Booking> expired = bookingRepository.findExpiredPending(cutoff)
                .stream()
                .filter(booking -> !paymentDeadlinePolicy.expiresAt(booking).isAfter(now))
                .toList();

        if (expired.isEmpty()) return;

        log.info("Found {} expired PENDING bookings to auto-cancel", expired.size());

        for (Booking booking : expired) {
            try {
                cancelBookingUseCase.cancelBooking(
                        booking.getId(), null, CancelledBy.SYSTEM,
                        "Auto-cancelled: payment not completed before " + paymentDeadlinePolicy.expiresAt(booking)
                );
            } catch (Exception e) {
                log.error("Failed to auto-cancel booking {}: {}", booking.getBookingCode(), e.getMessage());
            }
        }
    }

    /**
     * BR-CRON-002, BR-STATE-007: CONFIRMED bookings where check-in was more than
     * 24 hours ago and guest never checked in are marked NO_SHOW.
     * Runs every 1 hour.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void markNoShowBookings() {
        LocalDate checkInCutoff = LocalDate.now().minusDays(1); // 24h+ after check-in date
        List<Booking> candidates = bookingRepository.findNoShowCandidates(checkInCutoff);

        if (candidates.isEmpty()) return;

        log.info("Found {} bookings to mark as NO_SHOW", candidates.size());

        for (Booking booking : candidates) {
            booking.markNoShow();
            bookingRepository.save(booking);
            log.info("Booking marked NO_SHOW: code={}", booking.getBookingCode());
        }
    }
}
