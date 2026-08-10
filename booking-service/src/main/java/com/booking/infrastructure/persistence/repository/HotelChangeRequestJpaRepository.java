package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.HotelChangeRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HotelChangeRequestJpaRepository extends JpaRepository<HotelChangeRequestEntity, UUID> {

    Page<HotelChangeRequestEntity> findByHotelId(UUID hotelId, Pageable pageable);

    Page<HotelChangeRequestEntity> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    Optional<HotelChangeRequestEntity> findFirstByHotelIdAndStatusOrderByCreatedAtDesc(UUID hotelId, String status);
}
