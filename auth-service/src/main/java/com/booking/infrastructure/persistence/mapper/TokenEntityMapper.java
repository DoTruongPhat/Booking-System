package com.booking.infrastructure.persistence.mapper;

import com.booking.infrastructure.persistence.entity.TokenEntity;
import org.mapstruct.Mapper;

/**
 * TokenEntityMapper
 * → Convert giữa Token (domain) và TokenEntity (JPA)
 *
 * uses = {UserEntityMapper.class}
 * → TokenEntity có UserEntity (ManyToOne)
 * → MapStruct tự dùng UserEntityMapper
 *   để convert UserEntity → User
 */
@Mapper(componentModel = "spring",
        uses = {UserEntityMapper.class})
public interface TokenEntityMapper {

    /**
     * TokenEntity → Token domain
     * → Dùng khi đọc token từ DB
     */
    com.booking.domain.model.Token toDomain(TokenEntity entity);

    /**
     * Token domain → TokenEntity
     * → Dùng khi lưu token vào DB
     */
    TokenEntity toEntity(com.booking.domain.model.Token domain);
}