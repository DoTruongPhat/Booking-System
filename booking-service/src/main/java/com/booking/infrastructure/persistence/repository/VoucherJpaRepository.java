package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.VoucherEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoucherJpaRepository extends JpaRepository<VoucherEntity, UUID> {

    @Query("""
            SELECT v FROM VoucherEntity v
            WHERE (:hotelId IS NULL OR v.hotel.id = :hotelId)
              AND (:active IS NULL OR v.active = :active)
            """)
    Page<VoucherEntity> findForAdmin(
            @Param("hotelId") UUID hotelId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT v FROM VoucherEntity v
            WHERE (v.hotel IS NULL OR v.hotel.ownerUserId = :ownerUserId)
              AND (:active IS NULL OR v.active = :active)
            """)
    Page<VoucherEntity> findForHost(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("active") Boolean active,
            Pageable pageable);

    Optional<VoucherEntity> findByCodeIgnoreCase(String code);
}
