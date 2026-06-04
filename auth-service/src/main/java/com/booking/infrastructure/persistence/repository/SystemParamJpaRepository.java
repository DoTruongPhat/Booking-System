package com.booking.infrastructure.persistence.repository;

import com.booking.infrastructure.persistence.entity.SystemParamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemParamJpaRepository extends JpaRepository<SystemParamEntity, String> {
}
