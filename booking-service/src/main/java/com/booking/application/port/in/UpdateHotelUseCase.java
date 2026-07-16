package com.booking.application.port.in;

import com.booking.domain.model.Hotel;

import java.util.UUID;

public interface UpdateHotelUseCase {
    Hotel updateHotel(UUID hotelId, Hotel hotel, UUID ownerUserId);
}