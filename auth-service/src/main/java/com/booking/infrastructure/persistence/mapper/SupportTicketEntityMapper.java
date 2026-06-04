package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.SupportTicket;
import com.booking.infrastructure.persistence.entity.SupportTicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * SupportTicketEntityMapper
 * → Convert SupportTicket domain ↔ SupportTicketEntity
 *
 * status/priority: String (Entity) ↔ Enum (Domain)
 * → MapStruct tự convert String → Enum và ngược lại
 */
@Mapper(componentModel = "spring")
public interface SupportTicketEntityMapper {

    @Mapping(target = "status", expression =
            "java(com.booking.domain.enums.TicketStatus.valueOf(entity.getStatus()))")
    @Mapping(target = "priority", expression =
            "java(com.booking.domain.enums.TicketPriority.valueOf(entity.getPriority()))")
    SupportTicket toDomain(SupportTicketEntity entity);

    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    @Mapping(target = "priority", expression = "java(domain.getPriority().name())")
    SupportTicketEntity toEntity(SupportTicket domain);
}
