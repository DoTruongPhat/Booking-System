package com.booking.application.port.in;

import com.booking.domain.model.User;

import java.util.UUID;

public interface GetUserByIdUseCase {
    User getUserById(UUID id);
}
