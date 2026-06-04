package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.User;
import com.booking.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * UserEntityMapper
 * → Convert giữa User (domain) và UserEntity (JPA)
 *
 * uses = {RoleEntityMapper.class}
 * → UserEntity có Set<RoleEntity>
 * → MapStruct tự dùng RoleEntityMapper
 *   để convert Set<RoleEntity> → Set<Role>
 */
@Mapper(componentModel = "spring",
        uses = {RoleEntityMapper.class})
public interface UserEntityMapper {

    /**
     * UserEntity → User domain
     * → Dùng khi đọc user từ DB
     * → Service nhận User domain object
     */
    User toDomain(UserEntity entity);

    /**
     * User domain → UserEntity
     * → Dùng khi lưu user vào DB
     * → JPA nhận UserEntity để save
     */
    UserEntity toEntity(User domain);
}