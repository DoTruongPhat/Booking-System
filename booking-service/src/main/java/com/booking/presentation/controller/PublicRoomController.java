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
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicRoomController {

    private final SearchRoomUseCase searchRoomUseCase;
    private final RoomSearchDtoMapper mapper;

    @GetMapping("/api/rooms/search")
    public ResponseEntity<ApiResponse<Page<RoomSearchResponse>>> searchRooms(
            @RequestParam(required = false, defaultValue = "") String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "1") int guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @PageableDefault(size = 10) Pageable pageable) {

        LocalDate effectiveCheckIn = checkIn != null ? checkIn : LocalDate.now();
        LocalDate effectiveCheckOut = checkOut != null ? checkOut : effectiveCheckIn.plusDays(1);

        SearchCriteria criteria = new SearchCriteria(
                normalizeCity(city), effectiveCheckIn, effectiveCheckOut, guests, minPrice, maxPrice, minRating
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

    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return "";
        }

        String trimmed = city.trim();
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("\u0110", "D")
                .replace("\u0111", "d")
                .toLowerCase(Locale.ROOT)
                .replace("tp.", "")
                .replace("thanh pho", "")
                .trim();

        if (normalized.contains("ho chi minh") || normalized.contains("sai gon") || normalized.contains("saigon")) {
            return "Ho Chi Minh";
        }
        if (normalized.contains("phu quoc")) {
            return "Phu Quoc";
        }
        if (normalized.contains("da nang")) {
            return "Da Nang";
        }
        if (normalized.contains("ha noi") || normalized.contains("hanoi")) {
            return "Ha Noi";
        }

        return trimmed;
    }
}
