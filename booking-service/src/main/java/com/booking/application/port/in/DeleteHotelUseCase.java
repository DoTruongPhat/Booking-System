package com.booking.application.port.in;

import java.util.UUID;

public interface DeleteHotelUseCase {
    void deleteHotel(UUID hotelId);
    void deleteOwnHotel(UUID hotelId, UUID ownerUserId);
}
