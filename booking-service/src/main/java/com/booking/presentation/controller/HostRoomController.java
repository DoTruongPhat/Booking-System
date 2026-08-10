package com.booking.presentation.controller;

import com.booking.application.port.in.BlockRoomDatesUseCase;
import com.booking.application.port.in.CreateRoomUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.application.port.in.QueryRoomUseCase;
import com.booking.application.port.in.UpdateRoomUseCase;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.presentation.mapper.RoomDtoMapper;
import com.booking.presentation.request.BlockDatesRequest;
import com.booking.presentation.request.CreateRoomRequest;
import com.booking.presentation.request.UpdateRoomRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.RoomResponse;
import com.booking.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class HostRoomController {

    private final CreateRoomUseCase createRoomUseCase;
    private final UpdateRoomUseCase updateRoomUseCase;
    private final BlockRoomDatesUseCase blockRoomDatesUseCase;
    private final QueryRoomUseCase queryRoomUseCase;
    private final QueryHotelUseCase queryHotelUseCase;
    private final RoomDtoMapper mapper;

    @PostMapping("/api/host/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @PathVariable UUID hotelId,
            @Valid @RequestBody CreateRoomRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Room room = mapper.toDomain(request);
        Room created = createRoomUseCase.createRoom(hotelId, room, ownerUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/api/host/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody UpdateRoomRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Room updates = mapper.toDomain(request);
        Room updated = updateRoomUseCase.updateRoom(roomId, updates, ownerUserId);

        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(updated)));
    }

    @PostMapping("/api/host/rooms/{roomId}/availability/block")
    public ResponseEntity<ApiResponse<Void>> blockDates(
            @PathVariable UUID roomId,
            @Valid @RequestBody BlockDatesRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        blockRoomDatesUseCase.blockDates(roomId, request.getStartDate(), request.getEndDate(), ownerUserId);

        return ResponseEntity.ok(ApiResponse.success("Dates blocked successfully", null));
    }

    @GetMapping("/api/host/hotels/{hotelId}/rooms")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getRoomsByHotel(
            @PathVariable UUID hotelId,
            @PageableDefault(size = 10) Pageable pageable) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel hotel = queryHotelUseCase.getById(hotelId);
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }

        Page<RoomResponse> rooms = queryRoomUseCase
                .getByHotelId(hotelId, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(rooms));
    }
}
