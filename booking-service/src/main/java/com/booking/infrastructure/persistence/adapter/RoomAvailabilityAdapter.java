package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.RoomAvailabilityRepositoryPort;
import com.booking.domain.model.RoomAvailability;
import com.booking.infrastructure.persistence.entity.RoomAvailabilityEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import com.booking.infrastructure.persistence.mapper.RoomAvailabilityMapper;
import com.booking.infrastructure.persistence.repository.RoomAvailabilityJpaRepository;
import com.booking.infrastructure.persistence.repository.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomAvailabilityAdapter implements RoomAvailabilityRepositoryPort {

    private final RoomAvailabilityJpaRepository availabilityJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final RoomAvailabilityMapper mapper;

    @Override
    public List<RoomAvailability> saveAll(List<RoomAvailability> availabilities) {
        if (availabilities.isEmpty()) return List.of();

        UUID roomId = availabilities.get(0).getRoomId();
        RoomEntity roomEntity = roomJpaRepository.getReferenceById(roomId);

        List<RoomAvailabilityEntity> entities = availabilities.stream()
                .map(avail -> mapper.toEntity(avail, roomEntity))
                .toList();

        List<RoomAvailabilityEntity> saved = availabilityJpaRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RoomAvailability> findByRoomIdAndDateRange(UUID roomId, LocalDate startDate, LocalDate endDate) {
        return availabilityJpaRepository.findByRoomIdAndDateRange(roomId, startDate, endDate)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RoomAvailability> findByRoomIdAndDateRangeForUpdate(UUID roomId, LocalDate startDate, LocalDate endDate) {
        return availabilityJpaRepository.findByRoomIdAndDateRangeForUpdate(roomId, startDate, endDate)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void blockDates(UUID roomId, LocalDate startDate, LocalDate endDate) {
        availabilityJpaRepository.blockDates(roomId, startDate, endDate);
    }

    @Override
    public void decrementAvailability(UUID roomId, LocalDate startDate, LocalDate endDate, int numRooms) {
        availabilityJpaRepository.decrementAvailability(roomId, startDate, endDate, numRooms);
    }

    @Override
    public void incrementAvailability(UUID roomId, LocalDate startDate, LocalDate endDate, int numRooms) {
        availabilityJpaRepository.incrementAvailability(roomId, startDate, endDate, numRooms);
    }
}