package com.booking.application.port.in;

import com.booking.domain.model.Hotel;

import java.util.UUID;

public interface ApproveHotelUseCase {
    Hotel approveHotel(UUID hotelId);
}