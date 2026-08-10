package com.booking.application.service;

import com.booking.application.port.in.SearchRoomUseCase;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.application.port.out.RoomAvailabilityRepositoryPort;
import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.application.port.out.RoomSearchPort;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.booking.infrastructure.cache.config.RedisCacheConfig.CACHE_ROOM_DETAIL;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchRoomService implements SearchRoomUseCase {

    @Value("${app.availability.calendar-days:90}")
    private int calendarDays;

    private final RoomSearchPort roomSearchPort;
    private final RoomRepositoryPort roomRepository;
    private final HotelRepositoryPort hotelRepository;
    private final RoomAvailabilityRepositoryPort availabilityRepository;

    // ─── Search ──────────────────────────────────────────

    @Override
    public Page<RoomSearchResult> searchRooms(SearchCriteria criteria, Pageable pageable) {
        if (!criteria.checkOut().isAfter(criteria.checkIn())) {
            throw new CoreException(CoreErrorCode.BOOKING_INVALID_DATES);
        }

        log.info("Search rooms: city={}, checkIn={}, checkOut={}, guests={}, priceRange=[{}-{}], minRating={}",
                criteria.city(), criteria.checkIn(), criteria.checkOut(), criteria.guests(),
                criteria.minPrice(), criteria.maxPrice(), criteria.minRating());

        return roomSearchPort.search(criteria, pageable);
    }

    // ─── Room Detail ─────────────────────────────────────

    @Override
    @Cacheable(
            value = CACHE_ROOM_DETAIL,
            key = "'detail:roomId=' + #roomId",
            unless = "#result == null"
    )
    public RoomDetailResult getRoomDetail(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.ROOM_NOT_FOUND));

        Hotel hotel = hotelRepository.findById(room.getHotelId())
                .orElseThrow(() -> new CoreException(CoreErrorCode.HOTEL_NOT_FOUND));

        LocalDate today = LocalDate.now();
        List<RoomAvailability> availabilities = availabilityRepository
                .findByRoomIdAndDateRange(roomId, today, today.plusDays(calendarDays));

        return new RoomDetailResult(
                room,
                hotel.getName(),
                hotel.getCity(),
                hotel.getAddress(),
                hotel.getRating(),
                hotel.getAmenities(),
                hotel.getImages(),
                hotel.getCheckInTime(),
                hotel.getCheckOutTime(),
                availabilities
        );
    }
}
