package com.booking.application.service.serviceimpl;

import com.booking.application.port.in.*;
import com.booking.application.port.out.KeycloakAdminPort;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.port.out.UserKcLinkRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
import com.booking.application.service.PasswordService;
import com.booking.application.service.UserManagementService;
import com.booking.domain.exception.AuthException;
import com.booking.domain.exception.ErrorCode;
import com.booking.domain.exception.UserException;
import com.booking.domain.model.Role;
import com.booking.domain.model.User;
import com.booking.domain.model.UserKcLink;
import com.booking.shared.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        AdminResetPasswordUseCase,
        SetPasswordUseCase {

    private final UserRepositoryPort userRepository;
    private final RoleRepositoryPort roleRepository;
    private final PasswordService passwordService;
    private final UserKcLinkRepositoryPort kcLinkRepo;
    private final KeycloakAdminPort kcAdminClient;  // ← MỚI

    @Override
    public Page<User> getAllUsers(Pageable pageable) {
        log.info("[UserMgmt] Get all users, page: {}", pageable.getPageNumber());
        return userRepository.findAll(pageable);
    }

    @Override
    public User getUserById(UUID id) {
        log.info("[UserMgmt] Get user by id: {}", id);
        return userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));
    }

    @Override
    @Transactional
    public User updateUser(UUID id, String email, String timezone, Boolean active) {
        log.info("[UserMgmt] Update user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        if (email != null) user.setEmail(email);
        if (timezone != null) user.setTimezone(timezone);
        if (active != null) user.setActive(active);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        log.info("[UserMgmt] Deactivate user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public User assignRole(UUID userId, String roleCode) {
        log.info("[UserMgmt] Assign role {} to user: {}", roleCode, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new UserException(ErrorCode.CMN_005, ErrorCode.CMN_005_MSG + roleCode));
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    @Override
    public User getProfile(String username) {
        log.info("[UserMgmt] Get profile: {}", username);
        return userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));
    }

    @Override
    @Transactional
    public User updateProfile(String username, String email, String timezone,
                              String phone, String firstName, String lastName) {
        log.info("[UserMgmt] Update profile: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        if (email != null) user.setEmail(email);
        if (timezone != null) user.setTimezone(timezone);
        if (phone != null) user.setPhone(phone);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);

        return userRepository.save(user);
    }

    // ═══════════════════════════════════════════════════════════
    // CHANGE PASSWORD — sync KC
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        log.info("[UserMgmt] Change password for: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        boolean valid = passwordService.verify(
                currentPassword, user.getPasswordHash(), user.getPasswordSalt(), user.getUsername());
        if (!valid) {
            throw new AuthException(ErrorCode.AUTH_004, ErrorCode.AUTH_004_MSG);
        }

        // Save local
        PasswordService.HashedPassword hashed =
                passwordService.hash(newPassword, username, user.getPasswordSalt());
        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepository.save(user);

        // Sync KC (non-blocking)
        syncKcPassword(user.getId(), newPassword, "changePassword");

        log.info("[UserMgmt] Password changed for: {}", username);
    }

    // ═══════════════════════════════════════════════════════════
    // ADMIN RESET PASSWORD — sync KC
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void adminResetPassword(UUID userId, String newPassword) {
        log.info("[UserMgmt] Admin reset password for: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        PasswordService.HashedPassword hashed =
                passwordService.hash(newPassword, user.getUsername(), user.getPasswordSalt());
        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepository.save(user);

        // Sync KC (non-blocking)
        syncKcPassword(userId, newPassword, "adminResetPassword");

        log.info("[UserMgmt] Admin reset password for user: {}", userId);
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLETE PROFILE (SET PASSWORD) — sync KC
    // ═══════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void setPassword(String currentUsername, String newUsername, String newPassword) {
        log.info("[UserMgmt] Complete profile for: {}", MaskUtil.maskUsername(currentUsername));

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserException(ErrorCode.USR_001, ErrorCode.USR_001_MSG));

        if (user.getPasswordHash() != null) {
            throw new IllegalStateException("Password already set. Use change-password instead.");
        }

        UserKcLink link = kcLinkRepo.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Keycloak link not found for user"));

        String targetUsername = user.getUsername();

        // Đổi username nếu có
        if (newUsername != null && !newUsername.isBlank()
                && !newUsername.equals(currentUsername)) {
            if (newUsername.length() < 3 || newUsername.length() > 100) {
                throw new UserException(ErrorCode.USR_005, ErrorCode.USR_005_MSG);
            }
            if (!newUsername.matches("^[a-zA-Z0-9_.@]+$")) {  // Cho phép email format
                throw new UserException(ErrorCode.USR_010, ErrorCode.USR_010_MSG);
            }
            if (userRepository.existsByUsername(newUsername)) {
                throw new UserException(ErrorCode.USR_002, ErrorCode.USR_002_MSG);
            }

            KeycloakAdminPort.KcUserInfo kcUser = kcAdminClient.findUserByUsername(newUsername);
            if (kcUser != null && !link.getKcUserId().equals(kcUser.id())) {
                throw new UserException(ErrorCode.USR_002, ErrorCode.USR_002_MSG);
            }

            targetUsername = newUsername;
        }

        kcAdminClient.updateUserProfile(
                link.getKcUserId(),
                targetUsername,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
        kcAdminClient.resetPassword(link.getKcUserId(), newPassword);

        user.setUsername(targetUsername);

        // Hash password local
        PasswordService.HashedPassword hashed = passwordService.hash(
                newPassword, user.getUsername(), user.getId().toString());
        user.setPasswordHash(hashed.hash());
        user.setPasswordSalt(hashed.salt());
        userRepository.save(user);

        // Update KC link auth_source → LINKED
        link.markLinked();
        kcLinkRepo.save(link);

        log.info("[UserMgmt] Profile completed: {}", MaskUtil.maskUsername(user.getUsername()));
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER — sync password to KC (non-blocking)
    // ═══════════════════════════════════════════════════════════

    private void syncKcPassword(UUID userId, String newPassword, String source) {
        UserKcLink link = kcLinkRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Keycloak link not found for user"));

        kcAdminClient.resetPassword(link.getKcUserId(), newPassword);
        log.info("[UserMgmt] KC password synced ({}): kcUserId={}", source, link.getKcUserId());
    }
}
