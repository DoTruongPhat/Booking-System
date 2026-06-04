package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.Role;
import com.booking.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

/**
 * RoleEntityMapper
 * → Convert giữa Role (domain) và RoleEntity (JPA)
 *
 * uses = {PermissionEntityMapper.class}
 * → RoleEntity có Set<PermissionEntity>
 * → MapStruct tự dùng PermissionEntityMapper
 *   để convert Set<PermissionEntity> → Set<Permission>
 */
@Mapper(componentModel = "spring",
        uses = {PermissionEntityMapper.class})
public interface RoleEntityMapper {

    /**
     * RoleEntity → Role domain
     * → MapStruct tự convert permissions bên trong
     */
    Role toDomain(RoleEntity entity);

    /**
     * Role domain → RoleEntity
     */
    RoleEntity toEntity(Role domain);
}