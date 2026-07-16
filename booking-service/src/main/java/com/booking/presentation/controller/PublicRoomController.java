package com.booking.presentation.controller;

import com.booking.application.port.in.SearchRoomUseCase;
import com.booking.application.port.in.SearchRoomUseCase.RoomDetailResult;
import com.booking.application.port.in.SearchRoomUseCase.RoomSearchResult;
import com.booking.application.port.in.SearchRoomUseCase.SearchCriteria;
import com.booking.presentation.mapper.RoomSearchDtoMapper;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.RoomDetailResponse;
import com.booking.presentation.response.RoomSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicRoomController {

    private final SearchRoomUseCase searchRoomUseCase;
    private final RoomSearchDtoMapper mapper;

    @GetMapping("/api/rooms/search")
    public ResponseEntity<ApiResponse<Page<RoomSearchResponse>>> searchRooms(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @PageableDefault(size = 10) Pageable pageable) {

        SearchCriteria criteria = new SearchCriteria(
                city, checkIn, checkOut, guests, minPrice, maxPrice, minRating
        );

        Page<RoomSearchResult> results = searchRoomUseCase.searchRooms(criteria, pageable);
        Page<RoomSearchResponse> response = results.map(mapper::toSearchResponse);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/rooms/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetail(@PathVariable UUID roomId) {
        RoomDetailResult result = searchRoomUseCase.getRoomDetail(roomId);
        return ResponseEntity.ok(ApiResponse.success(mapper.toDetailResponse(result)));
    }
}