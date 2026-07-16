package com.booking.presentation.controller;

import com.booking.application.port.in.ApproveHotelUseCase;
import com.booking.application.port.in.QueryHotelUseCase;
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
        Hotel approved = approveHotelUseCase.approveHotel(id);
        return ResponseEntity.ok(ApiResponse.success("Hotel approved", mapper.toResponse(approved)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelResponse>> getHotel(@PathVariable UUID id) {
        Hotel hotel = queryHotelUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(mapper.toResponse(hotel)));
    }
}