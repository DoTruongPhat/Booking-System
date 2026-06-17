package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.KcTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KcTokenJpaRepository extends JpaRepository<KcTokenEntity, UUID> {

    Optional<KcTokenEntity> findByKcUserId(String kcUserId);

    Optional<KcTokenEntity> findByUserId(UUID userId);
}
