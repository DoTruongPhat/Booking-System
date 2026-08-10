package com.booking.application.service;

import com.booking.domain.model.Booking;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Component
public class PaymentDeadlinePolicy {

    @Value("${app.booking.pending-timeout-minutes:30}")
    private int minimumTimeoutMinutes;

    @Value("${app.booking.payment-deadline-days-before-checkin:1}")
    private int daysBeforeCheckIn;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    public Instant expiresAt(Booking booking) {
        Instant createdAt = booking.getCreatedAt() != null ? booking.getCreatedAt() : Instant.now();
        Instant minimumDeadline = createdAt.plus(minimumTimeoutMinutes, ChronoUnit.MINUTES);

        LocalDate checkInDate = booking.getCheckInDate();
        if (checkInDate == null) {
            return minimumDeadline;
        }

        Instant checkInDeadline = checkInDate
                .minusDays(Math.max(0, daysBeforeCheckIn))
                .plusDays(1)
                .atStartOfDay(ZoneId.of(timezone))
                .minusSeconds(1)
                .toInstant();

        return checkInDeadline.isAfter(minimumDeadline) ? checkInDeadline : minimumDeadline;
    }

    public int minimumTimeoutMinutes() {
        return minimumTimeoutMinutes;
    }

    public int daysBeforeCheckIn() {
        return daysBeforeCheckIn;
    }
}
