package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.Permission;
import com.booking.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;

/**
 * PermissionEntityMapper
 * → Convert giữa Permission (domain) và PermissionEntity (JPA)
 *
 * Tại sao tạo trước?
 * → RoleEntity có Set<PermissionEntity>
 * → RoleEntityMapper cần dùng PermissionEntityMapper
 * → Phải tạo trước để RoleEntityMapper dùng
 */
@Mapper(componentModel = "spring")
public interface PermissionEntityMapper {

    /**
     * PermissionEntity → Permission domain
     * → Gọi sau khi JPA query từ DB
     */
    Permission toDomain(PermissionEntity entity);

    /**
     * Permission domain → PermissionEntity
     * → Gọi trước khi JPA save vào DB
     */
    PermissionEntity toEntity(Permission domain);
}