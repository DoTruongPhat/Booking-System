package com.booking.payment.infrastructure.persistence.repository;

import com.booking.payment.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository
        extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.ProcessedEventId> {
}