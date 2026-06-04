package com.booking.application.port.in;

import com.booking.domain.model.User;

import java.util.UUID;

public interface UpdateUserUseCase {
    User updateUser(UUID id, String email, String timezone, Boolean active);
}
