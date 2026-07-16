package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.domain.model.Room;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.entity.RoomEntity;
import com.booking.infrastructure.persistence.mapper.RoomMapper;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import com.booking.infrastructure.persistence.repository.RoomJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoomAdapter implements RoomRepositoryPort {

    private final RoomJpaRepository roomJpaRepository;
    private final HotelJpaRepository hotelJpaRepository;
    private final RoomMapper mapper;

    @Override
    public Room save(Room room) {
        RoomEntity entity;

        if (room.getId() != null) {
            entity = roomJpaRepository.findById(room.getId()).orElse(null);
            if (entity != null) {
                mapper.updateEntity(entity, room);
            } else {
                HotelEntity hotelEntity = hotelJpaRepository.getReferenceById(room.getHotelId());
                entity = mapper.toEntity(room, hotelEntity);
            }
        } else {
            HotelEntity hotelEntity = hotelJpaRepository.getReferenceById(room.getHotelId());
            entity = mapper.toEntity(room, hotelEntity);
        }

        RoomEntity saved = roomJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Room> findById(UUID id) {
        return roomJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Room> findByHotelId(UUID hotelId, Pageable pageable) {
        return roomJpaRepository.findByHotelId(hotelId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Room> findAll(Pageable pageable) {
        return roomJpaRepository.findAll(pageable).map(mapper::toDomain);
    }
}