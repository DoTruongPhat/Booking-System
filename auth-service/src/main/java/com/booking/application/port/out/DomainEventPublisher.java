package com.booking.application.port.out;

import com.booking.domain.event.DomainEvent;

/**
 * DomainEventPublisher = Output Port
 *
 * Tại sao nằm ở port/out?
 * → Application muốn publish event ra bên ngoài
 * → Nhưng không biết infrastructure nào xử lý
 * → Kafka? RabbitMQ? Spring Events?
 * → Application chỉ biết interface này
 * → Infrastructure implement theo cách riêng
 */
public interface DomainEventPublisher {
    void publish(DomainEvent domainEvent);
}
