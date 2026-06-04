package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.*;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.PasswordService;
import com.booking.application.service.UserManagementService;
import com.booking.domain.exception.AuthException;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.UserException;
import com.booking.domain.model.Role;
import com.booking.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserManagementServiceImpl implements UserManagementService,
        GetAllUsersUseCase,
        GetUserByIdUseCase,
        UpdateUserUseCase,
        DeactivateUserUseCase,
        AssignRoleUseCase,
        GetProfileUseCase,
        UpdateProfileUseCase,
        ChangePasswordUseCase,
        AdminResetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordService passwordService;

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        log.info("[UserMgmt] Get all users, page: {}", pageable.getPageNumber());
        return userRepository.findAll(pageable);
    }

    @Override
    public User getUserById(UUID id) {
        log.info("[UserMgmt] Get user by id: {}", id);
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));
    }

    @Override
    @Transactional
    public User updateUser(UUID id, String email, String timezone, Boolean active) {
        log.info("[UserMgmt] Update user: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new UserException(
                ErrorCode.USR_001,
                ErrorCode.USR_001_MSG
        ));

        if(email != null) user.setEmail(email);
        if(timezone != null) user.setTimezone(timezone);
        if(active != null) user.setActive(active);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("[UserMgmt] Deactivate user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User assignRole(UUID userId, String roleCode) {
        log.info("[UserMgmt] Assign role {} to user: {}", roleCode, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001,
                        ErrorCode.USR_001_MSG
                ));

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new UserException(
                        ErrorCode.CMN_005,
                        ErrorCode.CMN_005_MSG + roleCode
                ));

        // Thêm role mới vào set hiện tại
        Set<Role> roles = new HashSet<>(user.getRoles());
        roles.add(role);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public User getProfile(String username) {
        log.info("[UserMgmt] Get profile: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));
    }

    @Override
    @Transactional
    public User updateProfile(String username, String email, String timezone) {
        log.info("[UserMgmt] Update profile: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        if (email != null) user.setEmail(email);
        if (timezone != null) user.setTimezone(timezone);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username,
                               String currentPassword,
                               String newPassword) {
        log.info("[UserMgmt] Change password for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        // Verify current password
        boolean valid = passwordService.verify(
                currentPassword,
                user.getPasswordHash(),
                user.getPasswordSalt(),
                user.getUsername());

        if (!valid) {
            throw new AuthException(
                    ErrorCode.AUTH_004,
                    ErrorCode.AUTH_004_MSG);
        }

        // Hash new password
        PasswordService.HashedPassword hashed =
                passwordService.hash(newPassword, username, user.getPasswordSalt());

        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepository.save(user);

        log.info("[UserMgmt] Password changed for: {}", username);
    }

    @Override
    @Transactional
    public void adminResetPassword(UUID userId, String newPassword) {
        log.info("[UserMgmt] Admin reset password for: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(
                        ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        // Hash new password
        PasswordService.HashedPassword hashed =
                passwordService.hash(newPassword, user.getUsername(), user.getPasswordSalt());

        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepository.save(user);

        log.info("[UserMgmt] Admin reset password for user: {}", userId);
    }
}
