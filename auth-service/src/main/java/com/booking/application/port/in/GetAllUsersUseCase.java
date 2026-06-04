package com.booking.application.port.in;

import com.booking.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetAllUsersUseCase {
    Page<User> getAllUsers(Pageable pageable);
}
