package com.booking.presentation.response;

import java.math.BigDecimal;

public record HostDashboardResponse(
        long totalUsers,
        long totalHotels,
        long activeHotels,
        long pendingHotels,
        long totalRooms,
        long totalRoomTypes,
        long availableRoomTypes,
        long totalBookings,
        long activeBookings,
        long pendingBookings,
        long upcomingCheckIns,
        BigDecimal totalRevenue,
        String status,
        String message
) {
}
