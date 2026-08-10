package com.booking.presentation.controller;

import com.booking.application.port.in.ApproveHotelUseCase;
import com.booking.application.port.in.DeactivateHotelUseCase;
import com.booking.application.port.in.DeleteHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.presentation.mapper.HotelDtoMapper;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.HotelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
public class AdminHotelController {

    private final ApproveHotelUseCase approveHotelUseCase;
    private final DeactivateHotelUseCase deactivateHotelUseCase;
    private final DeleteHotelUseCase deleteHotelUseCase;
    private final QueryHotelUseCase queryHotelUseCase;
    private final HotelDtoMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelResponse>>> getAllHotels(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<HotelResponse> hotels = queryHotelUseCase
                .getAll(status, pageable)
                .map(mapper::toResponse);

        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<HotelResponse>> approveHotel(@PathVariable UUID id) {
        throw new CoreException(
                CoreErrorCode.INVALID_REQUEST,
                "Hotel approval must be completed through the Camunda workflow task"
        );
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<HotelResponse>> deactivateHotel(@PathVariable UUID id) {
        Hotel deactivated = deactivateHotelUseCase.deactivateHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deactivated", mapper.toResponse(deactivated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHotel(@PathVariable UUID id) {
        deleteHotelUseCase.deleteHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel deleted", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotel(@PathVariable UUID id) {
        Hotel hotel = queryHotelUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(hotel)));
    }
}
