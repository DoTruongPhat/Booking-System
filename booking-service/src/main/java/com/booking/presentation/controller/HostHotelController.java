package com.booking.presentation.controller;

import com.booking.application.port.in.CreateHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.application.port.in.UpdateHotelUseCase;
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
        Hotel updates = mapper.toDomain(request);
        Hotel updated = updateHotelUseCase.updateHotel(id, updates, ownerUserId);

        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(updated)));
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
        Hotel hotel = queryHotelUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(hotel)));
    }
}