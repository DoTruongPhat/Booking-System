package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.PromotionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PromotionJpaRepository extends JpaRepository<PromotionEntity, UUID> {

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE (:hotelId IS NULL OR p.hotel.id = :hotelId)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<PromotionEntity> findForAdmin(
            @Param("hotelId") UUID hotelId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT p FROM PromotionEntity p
            WHERE (p.hotel IS NULL OR p.hotel.ownerUserId = :ownerUserId)
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<PromotionEntity> findForHost(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("active") Boolean active,
            Pageable pageable);
}
