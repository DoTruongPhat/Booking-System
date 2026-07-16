package com.booking.application.service;

import com.booking.application.port.in.HostDashboardUseCase;
import com.booking.infrastructure.persistence.repository.BookingJpaRepository;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.RoomJpaRepository;
import com.booking.presentation.response.HostDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HostDashboardService implements HostDashboardUseCase {

    private final HotelJpaRepository hotelRepository;
    private final RoomJpaRepository roomRepository;
    private final BookingJpaRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public HostDashboardResponse getDashboard(UUID ownerUserId) {
        long totalHotels = hotelRepository.countByOwnerUserId(ownerUserId);
        long activeHotels = hotelRepository.countByOwnerUserIdAndStatus(ownerUserId, "ACTIVE");
        long pendingHotels = hotelRepository.countByOwnerUserIdAndStatus(ownerUserId, "PENDING_APPROVAL");
        long totalRoomTypes = roomRepository.countByOwnerUserId(ownerUserId);
        long totalRooms = roomRepository.sumTotalRoomsByOwnerUserId(ownerUserId);
        long availableRoomTypes = roomRepository.countByOwnerUserIdAndStatus(ownerUserId, "AVAILABLE");
        long totalBookings = bookingRepository.countByOwnerUserId(ownerUserId);
        long pendingBookings = bookingRepository.countByOwnerUserIdAndStatus(ownerUserId, "PENDING");
        long confirmedBookings = bookingRepository.countByOwnerUserIdAndStatus(ownerUserId, "CONFIRMED");
        long upcomingCheckIns = bookingRepository.countUpcomingCheckIns(
                ownerUserId,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );
        BigDecimal totalRevenue = bookingRepository.sumPaidRevenueByOwnerUserId(ownerUserId);

        return new HostDashboardResponse(
                totalHotels,
                totalHotels,
                activeHotels,
                pendingHotels,
                totalRooms,
                totalRoomTypes,
                availableRoomTypes,
                totalBookings,
                pendingBookings + confirmedBookings,
                pendingBookings,
                upcomingCheckIns,
                totalRevenue,
                "success",
                "Host dashboard loaded"
        );
    }
}
