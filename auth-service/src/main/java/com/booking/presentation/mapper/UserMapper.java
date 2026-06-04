package com.booking.presentation.mapper;


import com.booking.domain.model.User;
import com.booking.presentation.request.RegisterRequest;
import com.booking.presentation.response.RegisterResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convert RegisterRequest → User entity
     * Bỏ qua các field tự sinh hoặc set riêng
     */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "passwordSalt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    @Mapping(target = "totpSecret", ignore = true)
    @Mapping(target = "timezone", source = "timeZone")
    User toEntity(RegisterRequest request);

    /**
     * Convert User entity → RegisterResponse
     */
    @Mapping(target = "id", expression = "java(user.getId().toString())")
    @Mapping(target = "message", ignore = true)
    RegisterResponse toResponse(User user);

}
