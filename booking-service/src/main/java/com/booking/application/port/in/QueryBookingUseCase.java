package com.booking.application.port.in;

import com.booking.domain.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QueryBookingUseCase {

    Booking getById(UUID bookingId);

    Page<Booking> getByUserId(UUID userId, Pageable pageable);

    Page<Booking> getByHotelId(UUID hotelId, Pageable pageable);

    Page<Booking> getByHotelId(UUID hotelId, String status, Pageable pageable);

    Page<Booking> getByOwnerUserId(UUID ownerUserId, String status, Pageable pageable);

    Page<Booking> getAll(String status, Pageable pageable);
}
