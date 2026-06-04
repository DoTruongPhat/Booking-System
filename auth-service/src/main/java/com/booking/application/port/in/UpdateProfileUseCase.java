package com.booking.application.port.in;

import com.booking.domain.model.User;


public interface UpdateProfileUseCase {
    User updateProfile(String username, String email, String timezone);
}
