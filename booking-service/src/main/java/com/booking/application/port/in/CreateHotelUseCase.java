package com.booking.application.port.in;

import com.booking.domain.model.Hotel;

import java.util.UUID;

public interface CreateHotelUseCase {
    Hotel createHotel(Hotel hotel, UUID ownerUserId);
}