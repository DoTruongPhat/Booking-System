package com.booking.payment.infrastructure.persistence.adapter;

import com.booking.payment.application.port.out.ProcessedEventRepositoryPort;
import com.booking.payment.domain.model.ProcessedEvent;
import com.booking.payment.infrastructure.persistence.entity.ProcessedEventEntity;
import com.booking.payment.infrastructure.persistence.mapper.PaymentMapper;
import com.booking.payment.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedEventAdapter implements ProcessedEventRepositoryPort {

    private final ProcessedEventJpaRepository jpaRepository;
    private final PaymentMapper mapper;

    @Override
    public boolean exists(String eventType, String eventId) {
        var compositeId = new ProcessedEventEntity.ProcessedEventId(eventType, eventId);
        return jpaRepository.existsById(compositeId);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent event) {
        var entity = mapper.toEntity(event);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}