package com.booking.presentation.controller;

import com.booking.application.port.in.QueryRoomUseCase;
import com.booking.presentation.mapper.RoomDtoMapper;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminRoomController {

    private final QueryRoomUseCase queryRoomUseCase;
    private final RoomDtoMapper mapper;

    @GetMapping("/api/admin/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getRoomsByHotel(
            @PathVariable UUID hotelId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<RoomResponse> rooms = queryRoomUseCase
                .getByHotelId(hotelId, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
