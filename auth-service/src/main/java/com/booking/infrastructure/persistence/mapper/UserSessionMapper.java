package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.UserSession;
import com.booking.infrastructure.persistence.entity.UserSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserSessionMapper {

    UserSession toDomain(UserSessionEntity entity);

    UserSessionEntity toEntity(UserSession domain);
}
