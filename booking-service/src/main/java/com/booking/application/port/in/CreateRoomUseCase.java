package com.booking.application.port.in;

import com.booking.domain.model.Room;

import java.util.UUID;

public interface CreateRoomUseCase {
    Room createRoom(UUID hotelId, Room room, UUID ownerUserId);
}