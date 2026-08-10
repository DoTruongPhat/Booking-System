package com.booking.presentation.controller;

import com.booking.application.service.HotelService;
import com.booking.domain.model.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class PublicHotelController {

    private final HotelService hotelService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listHotels(Pageable pageable) {
        Page<Hotel> hotels = hotelService.getAll("ACTIVE", pageable);
        return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 200,
                "message", "Success",
                "data", hotels
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHotel(@PathVariable UUID id) {
        Hotel hotel = hotelService.getById(id);
        return ResponseEntity.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 200,
                "message", "Success",
                "data", hotel
        ));
    }
}