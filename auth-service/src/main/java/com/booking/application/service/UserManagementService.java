package com.booking.application.service;

import com.booking.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserManagementService {
    Page<User> getAllUsers(Pageable pageable);
    User getUserById(UUID id);
    User updateUser(UUID id, String email, String timezone, Boolean active);
    void deactivateUser(UUID userId);
    User assignRole(UUID userId, String roleCode);
    User getProfile(String username);
    User updateProfile(String username, String email, String timezone);

    void changePassword(String username, String currentPassword, String newPassword);
    void adminResetPassword(UUID userId, String newPassword);


}
