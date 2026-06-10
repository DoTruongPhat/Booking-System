package com.booking.presentation.controller;

import com.booking.application.port.in.ChangePasswordUseCase;
import com.booking.application.port.in.GetProfileUseCase;
import com.booking.application.port.in.UpdateProfileUseCase;
import com.booking.domain.model.User;
import com.booking.presentation.request.ChangePasswordRequest;
import com.booking.presentation.request.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ProfileController = User xem/sửa profile của mình
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@Log4j2
public class ProfileController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    @GetMapping
    public ResponseEntity<User> getProfile() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(
                getProfileUseCase.getProfile(username));
    }

    @PutMapping
    public ResponseEntity<User> updateProfile(
            @RequestBody UpdateUserRequest request) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(
                updateProfileUseCase.updateProfile(
                        username,
                        request.getEmail(),
                        request.getTimezone()));
    }
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        changePasswordUseCase.changePassword(
                username,
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
