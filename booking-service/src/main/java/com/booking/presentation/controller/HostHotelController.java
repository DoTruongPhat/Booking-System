package com.booking.presentation.controller;

import com.booking.application.port.in.CreateHotelUseCase;
import com.booking.application.port.in.DeactivateHotelUseCase;
import com.booking.application.port.in.DeleteHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.application.port.in.UpdateHotelUseCase;
import com.booking.domain.enums.HotelStatus;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.presentation.mapper.HotelDtoMapper;
import com.booking.presentation.request.CreateHotelRequest;
import com.booking.presentation.request.UpdateHotelRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.HotelResponse;
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
@RequestMapping("/api/host/hotels")
@RequiredArgsConstructor
public class HostHotelController {

    private final CreateHotelUseCase createHotelUseCase;
    private final DeactivateHotelUseCase deactivateHotelUseCase;
    private final DeleteHotelUseCase deleteHotelUseCase;
    private final UpdateHotelUseCase updateHotelUseCase;
    private final QueryHotelUseCase queryHotelUseCase;
    private final HotelDtoMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<HotelResponse>> createHotel(
            @Valid @RequestBody CreateHotelRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel hotel = mapper.toDomain(request);
        Hotel created = createHotelUseCase.createHotel(hotel, ownerUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toResponse(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> updateHotel(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateHotelRequest request) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel current = queryHotelUseCase.getById(id);
        if (!current.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
        Hotel updates = mapper.toDomain(request);
        Hotel updated = updateHotelUseCase.updateHotel(id, updates, ownerUserId);

        String message = current.getStatus() == HotelStatus.ACTIVE
                ? "Hotel update request submitted for admin approval"
                : "Hotel updated";
        return ResponseEntity.ok(ApiResponse.success(message, mapper.toResponse(updated)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelResponse>>> getMyHotels(
            @PageableDefault(size = 10) Pageable pageable) {

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Page<HotelResponse> hotels = queryHotelUseCase
                .getByOwner(ownerUserId, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotel(@PathVariable UUID id) {
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel hotel = queryHotelUseCase.getById(id);
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(hotel)));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<HotelResponse>> deactivateHotel(@PathVariable UUID id) {
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        Hotel hotel = deactivateHotelUseCase.deactivateOwnHotel(id, ownerUserId);
        return ResponseEntity.ok(ApiResponse.success("Hotel deactivated", mapper.toResponse(hotel)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable UUID id) {
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        deleteHotelUseCase.deleteOwnHotel(id, ownerUserId);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted", null));
    }
}
