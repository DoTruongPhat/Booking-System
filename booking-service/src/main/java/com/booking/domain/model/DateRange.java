package com.booking.domain.model;

import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new CoreException(CoreErrorCode.INVALID_REQUEST, "Check-in and check-out dates are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new CoreException(CoreErrorCode.BOOKING_INVALID_DATES);
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new CoreException(CoreErrorCode.BOOKING_PAST_CHECKIN);
        }
        if (checkIn.isAfter(LocalDate.now().plusDays(365))) {
            throw new CoreException(CoreErrorCode.BOOKING_TOO_FAR_AHEAD);
        }
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Returns all dates in the stay (check-in inclusive, check-out exclusive).
     * Example: checkIn=July 10, checkOut=July 13 → [July 10, July 11, July 12]
     */
    public List<LocalDate> stayDates() {
        return checkIn.datesUntil(checkOut).toList();
    }
}