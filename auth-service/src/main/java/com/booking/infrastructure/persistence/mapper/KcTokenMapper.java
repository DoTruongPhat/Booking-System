package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.KcToken;
import com.booking.infrastructure.persistence.entity.KcTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KcTokenMapper {
    KcToken toDomain(KcTokenEntity entity);

    KcTokenEntity toEntity(KcToken domain);
}
