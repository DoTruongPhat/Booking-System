package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.SystemParam;
import com.booking.infrastructure.persistence.entity.SystemParamEntity;
import org.mapstruct.Mapper;

/**
 * SystemParamEntityMapper
 * → Convert giữa SystemParam (domain) và SystemParamEntity (JPA)
 * → Đơn giản nhất vì không có relationship
 */
@Mapper(componentModel = "spring")
public interface SystemParamEntityMapper {

    /**
     * SystemParamEntity → SystemParam domain
     */
    SystemParam toDomain(SystemParamEntity entity);

    /**
     * SystemParam domain → SystemParamEntity
     */
    SystemParamEntity toEntity(SystemParam domain);
}