package com.booking.infrastructure.persistence.mapper;

import com.booking.domain.model.TokenBlacklist;
import com.booking.infrastructure.persistence.entity.TokenBlacklistEntity;
import org.mapstruct.Mapper;

/**
 * TokenBlacklistMapper
 * → Convert giữa TokenBlacklist (domain) ↔ TokenBlacklistEntity (JPA)
 *
 * Không có nested object → mapping đơn giản, MapStruct tự match tên field:
 *   jti, userId, blacklistedAt, expiresAt, reason
 */
@Mapper(componentModel = "spring")
public interface TokenBlacklistMapper {

    /**
     * Entity → Domain
     */
    TokenBlacklist toDomain(TokenBlacklistEntity entity);

    /**
     * Domain → Entity
     * Lưu ý: blacklistedAt sẽ được Hibernate @CreationTimestamp set
     */
    TokenBlacklistEntity toEntity(TokenBlacklist domain);
}   