package com.booking.application.port.in;

import com.booking.domain.model.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QueryHotelUseCase {

    Hotel getById(UUID hotelId);

    Page<Hotel> getByOwner(UUID ownerUserId, Pageable pageable);

    Page<Hotel> getAll(String status, Pageable pageable);
}