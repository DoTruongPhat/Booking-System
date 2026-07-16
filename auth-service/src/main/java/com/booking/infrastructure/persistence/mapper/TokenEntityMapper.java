package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.Token;
import com.booking.infrastructure.persistence.entity.TokenEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * TokenEntityMapper
 * → Convert giữa Token (domain) ↔ TokenEntity (JPA)
 *
 * uses = {UserEntityMapper.class}
 * → TokenEntity có UserEntity (ManyToOne)
 * → MapStruct tự dùng UserEntityMapper để convert
 *
 * V10 changes:
 * - Bỏ mapping tokenEncrypted (đã DROP)
 * - Bỏ mapping lastUsedAt (đã DROP)
 * - Auto-map expiresAt (cùng tên ở 2 bên)
 */
@Mapper(componentModel = "spring", uses = {UserEntityMapper.class})
public interface TokenEntityMapper {

    /**
     * TokenEntity → Token domain
     * → Dùng khi đọc token từ DB
     */
    Token toDomain(TokenEntity entity);

    /**
     * Token domain → TokenEntity
     * → Dùng khi lưu token vào DB
     *
     * Lưu ý:
     * - createdAt: skip (Hibernate @CreationTimestamp tự set)
     *   Nếu domain có sẵn createdAt (load từ DB rồi sửa) → MapStruct vẫn copy
     *   nhưng @CreationTimestamp chỉ chạy lúc INSERT đầu tiên
     */
    TokenEntity toEntity(Token domain);
}