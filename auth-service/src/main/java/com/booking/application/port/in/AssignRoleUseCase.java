package com.booking.application.port.in;

import com.booking.domain.model.User;

import java.util.UUID;

public interface AssignRoleUseCase {
    User assignRole(UUID userId, String roleCode);
}
