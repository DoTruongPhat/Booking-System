package com.booking.presentation.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class RoomDetailResponse {

    // Room info
    private UUID roomId;
    private String roomName;
    private String roomType;
    private String roomDescription;
    private int capacity;
    private BigDecimal basePrice;
    private int totalRooms;
    private List<String> roomAmenities;
    private String roomStatus;
    private List<String> roomImages;

    // Hotel info
    private UUID hotelId;
    private String hotelName;
    private String hotelCity;
    private String hotelAddress;
    private BigDecimal hotelRating;
    private List<String> hotelAmenities;
    private List<String> hotelImages;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    // Availability (next 30 days)
    private List<AvailabilitySlot> availabilities;

    @Getter
    @Setter
    @Builder
    public static class AvailabilitySlot {
        private LocalDate date;
        private int availableCount;
        private BigDecimal effectivePrice;
        private String status;
    }
}