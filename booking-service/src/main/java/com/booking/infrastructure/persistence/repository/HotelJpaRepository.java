package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.HotelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface HotelJpaRepository extends JpaRepository<HotelEntity, UUID> {

    Page<HotelEntity> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    long countByOwnerUserId(UUID ownerUserId);

    long countByOwnerUserIdAndStatus(UUID ownerUserId, String status);

    @Query("SELECT h FROM HotelEntity h WHERE (:status IS NULL OR h.status = :status)")
    Page<HotelEntity> findAllByStatus(@Param("status") String status, Pageable pageable);

    boolean existsByNameAndCity(String name, String city);
}
