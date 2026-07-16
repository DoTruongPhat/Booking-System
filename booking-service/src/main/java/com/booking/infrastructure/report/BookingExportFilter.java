package com.booking.infrastructure.report;

import java.time.LocalDate;
import java.util.UUID;

public record BookingExportFilter(
        LocalDate from,
        LocalDate to,
        UUID hotelId,
        String status
) {
    public static BookingExportFilter of(LocalDate from, LocalDate to, UUID hotelId, String status) {
        return new BookingExportFilter(from, to, hotelId, status);
    }
}