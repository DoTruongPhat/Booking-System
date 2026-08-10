package com.booking.application.service;

import com.booking.application.port.in.BlockRoomDatesUseCase;
import com.booking.application.port.in.CreateRoomUseCase;
import com.booking.application.port.in.QueryRoomUseCase;
import com.booking.application.port.in.UpdateRoomUseCase;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.application.port.out.RoomAvailabilityRepositoryPort;
import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.domain.enums.AvailabilityStatus;
import com.booking.domain.exception.CoreErrorCode;
import com.booking.domain.exception.CoreException;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.domain.model.RoomAvailability;
import com.booking.domain.validation.RoomValidator;
import com.booking.infrastructure.cache.RoomCacheAdapter;
import com.booking.infrastructure.persistence.repository.RoomTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomService implements CreateRoomUseCase, UpdateRoomUseCase,
        BlockRoomDatesUseCase, QueryRoomUseCase {

    private static final int AVAILABILITY_DAYS = 30;

    private final RoomRepositoryPort roomRepository;
    private final RoomAvailabilityRepositoryPort availabilityRepository;
    private final HotelRepositoryPort hotelRepository;
    private final RoomValidator roomValidator;
    private final RoomCacheAdapter roomCacheAdapter;
    private final RoomTypeJpaRepository roomTypeRepository;

    // ─── CreateRoomUseCase ───────────────────

    @Override
    public Room createRoom(UUID hotelId, Room room, UUID ownerUserId) {
        Hotel hotel = findHotelOrThrow(hotelId);
        verifyOwnership(hotel, ownerUserId);

        room.setHotelId(hotelId);
        room.setRoomType(normalizeRoomType(room.getRoomType()));
        validateRoomType(hotelId, room.getRoomType());
        Room saved = roomRepository.save(room);

        // Auto-generate availability for next 30 days
        List<RoomAvailability> availabilities = generateAvailability(
                saved.getId(), saved.getTotalRooms(), AVAILABILITY_DAYS
        );
        availabilityRepository.saveAll(availabilities);

        log.info("Room created: id={}, hotel={}, type={}, totalRooms={}, availability={}days",
                saved.getId(), hotelId, saved.getRoomType(), saved.getTotalRooms(), AVAILABILITY_DAYS);

        roomCacheAdapter.invalidateSearchResults();

        return saved;
    }

    // ─── UpdateRoomUseCase ───────────────────

    @Override
    public Room updateRoom(UUID roomId, Room updates, UUID ownerUserId) {
        Room room = findRoomOrThrow(roomId);

        // BR-ROOM-012: validate totalRooms reduction
        if (updates.getTotalRooms() < room.getTotalRooms()) {
            roomValidator.validateTotalRoomsReduction(roomId, updates.getTotalRooms());
        }

        Hotel hotel = findHotelOrThrow(room.getHotelId());
        verifyOwnership(hotel, ownerUserId);

        updates.setRoomType(normalizeRoomType(updates.getRoomType()));
        validateRoomType(room.getHotelId(), updates.getRoomType());
        room.setRoomType(updates.getRoomType());
        room.setName(updates.getName());
        room.setDescription(updates.getDescription());
        room.setCapacity(updates.getCapacity());
        room.setBasePrice(updates.getBasePrice());
        room.setTotalRooms(updates.getTotalRooms());
        room.setAmenities(updates.getAmenities());
        room.setStatus(updates.getStatus());
        room.setImages(updates.getImages());

        Room saved = roomRepository.save(room);

        log.info("Room updated: id={}, name={}", saved.getId(), saved.getName());

        roomCacheAdapter.invalidateRoomDetail(saved.getId());
        roomCacheAdapter.invalidateSearchResults();

        return saved;
    }
    // ─── BlockRoomDatesUseCase ───────────────

    @Override
    public void blockDates(UUID roomId, LocalDate startDate, LocalDate endDate, UUID ownerUserId) {
        Room room = findRoomOrThrow(roomId);
        Hotel hotel = findHotelOrThrow(room.getHotelId());
        verifyOwnership(hotel, ownerUserId);

        if (!endDate.isAfter(startDate)) {
            throw new CoreException(CoreErrorCode.BOOKING_INVALID_DATES,
                    "End date must be after start date");
        }

        availabilityRepository.blockDates(roomId, startDate, endDate);
        roomCacheAdapter.invalidateOnBookingChange(roomId);
        log.info("Dates blocked: room={}, from={} to={}", roomId, startDate, endDate);
    }

    // ─── QueryRoomUseCase ────────────────────

    @Override
    @Transactional(readOnly = true)
    public Room getById(UUID roomId) {
        return findRoomOrThrow(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Room> getByHotelId(UUID hotelId, Pageable pageable) {
        return roomRepository.findByHotelId(hotelId, pageable);
    }

    // ─── Private helpers ─────────────────────

    private Hotel findHotelOrThrow(UUID hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.HOTEL_NOT_FOUND));
    }

    private Room findRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CoreException(CoreErrorCode.ROOM_NOT_FOUND));
    }

    private void verifyOwnership(Hotel hotel, UUID ownerUserId) {
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new CoreException(CoreErrorCode.HOTEL_NOT_OWNED);
        }
    }

    private void validateRoomType(UUID hotelId, String roomType) {
        if (roomType == null || roomType.isBlank()) {
            throw new CoreException(CoreErrorCode.ROOM_TYPE_NOT_FOUND);
        }
        if (!roomTypeRepository.existsActiveForHotelOrGlobal(hotelId, roomType)) {
            throw new CoreException(
                    CoreErrorCode.ROOM_TYPE_NOT_FOUND,
                    "Room type does not exist for this hotel or global catalog: " + roomType
            );
        }
    }

    private String normalizeRoomType(String value) {
        return value == null ? null : value.trim().toUpperCase().replaceAll("\\s+", "_");
    }

    private List<RoomAvailability> generateAvailability(UUID roomId, int totalRooms, int days) {
        List<RoomAvailability> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days; i++) {
            RoomAvailability avail = new RoomAvailability();
            avail.setRoomId(roomId);
            avail.setDate(today.plusDays(i));
            avail.setAvailableCount(totalRooms);
            avail.setStatus(AvailabilityStatus.AVAILABLE);
            list.add(avail);
        }

        return list;
    }
}
