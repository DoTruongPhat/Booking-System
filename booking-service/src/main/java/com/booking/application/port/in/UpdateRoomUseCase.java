package com.booking.application.port.in;

import com.booking.domain.model.Room;

import java.util.UUID;

public interface UpdateRoomUseCase {
    Room updateRoom(UUID roomId, Room updates, UUID ownerUserId);
}