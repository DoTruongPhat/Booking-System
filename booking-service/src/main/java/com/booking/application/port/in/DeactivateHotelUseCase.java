package com.booking.application.port.in;

import com.booking.domain.model.Hotel;

import java.util.UUID;

public interface DeactivateHotelUseCase {
    Hotel deactivateHotel(UUID hotelId);
    Hotel deactivateOwnHotel(UUID hotelId, UUID ownerUserId);
}
