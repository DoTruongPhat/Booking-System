package com.booking.application.port.in;

import com.booking.domain.model.User;

public interface GetProfileUseCase {
    User getProfile(String username);
}
