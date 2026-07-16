package com.booking.infrastructure.persistence.adapter;

import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.domain.model.Hotel;
import com.booking.infrastructure.persistence.entity.HotelEntity;
import com.booking.infrastructure.persistence.mapper.HotelMapper;
import com.booking.infrastructure.persistence.repository.HotelJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HotelAdapter implements HotelRepositoryPort {

    private final HotelJpaRepository jpaRepository;
    private final HotelMapper mapper;

    @Override
    public Hotel save(Hotel hotel) {
        HotelEntity entity;

        if (hotel.getId() != null) {
            // Update existing
            entity = jpaRepository.findById(hotel.getId()).orElse(null);
            if (entity != null) {
                mapper.updateEntity(entity, hotel);
            } else {
                entity = mapper.toEntity(hotel);
            }
        } else {
            // Create new
            entity = mapper.toEntity(hotel);
        }

        HotelEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Hotel> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Hotel> findByOwnerUserId(UUID ownerUserId, Pageable pageable) {
        return jpaRepository.findByOwnerUserId(ownerUserId, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Hotel> findAll(String status, Pageable pageable) {
        return jpaRepository.findAllByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNameAndCity(String name, String city) {
        return jpaRepository.existsByNameAndCity(name, city);
    }
}