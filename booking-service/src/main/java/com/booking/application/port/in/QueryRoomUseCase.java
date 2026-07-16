package com.booking.application.port.in;

import com.booking.domain.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QueryRoomUseCase {

    Room getById(UUID roomId);

    Page<Room> getByHotelId(UUID hotelId, Pageable pageable);
}