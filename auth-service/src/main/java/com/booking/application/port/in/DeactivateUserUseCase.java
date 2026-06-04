package com.booking.application.port.in;

import java.util.UUID;

public interface DeactivateUserUseCase {
    void deactivateUser(UUID userId);
}
