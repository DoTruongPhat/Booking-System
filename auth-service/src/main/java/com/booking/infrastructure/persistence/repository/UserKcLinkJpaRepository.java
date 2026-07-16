package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.UserKcLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserKcLinkJpaRepository extends JpaRepository<UserKcLinkEntity, UUID> {

    Optional<UserKcLinkEntity> findByKcUserId(String kcUserId);

    Optional<UserKcLinkEntity> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}