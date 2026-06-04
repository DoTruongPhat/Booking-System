package com.booking.presentation.controller;


import com.booking.application.port.in.*;
import com.booking.domain.model.SupportTicket;
import com.booking.domain.model.User;
import com.booking.presentation.request.AdminResetPasswordRequest;
import com.booking.presentation.request.AssignRoleRequest;
import com.booking.presentation.request.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Log4j2
public class AdminController {

    // Session
    private final RevokeAllSessionsUseCase revokeAllSessionsUseCase;
    private final RevokeSessionUseCase revokeSessionUseCase;

    // User Management
    private final GetAllUsersUseCase getAllUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final AssignRoleUseCase assignRoleUseCase;
    private final AdminResetPasswordUseCase adminResetPasswordUseCase;

    // Ticket Management
    private final GetTicketsUseCase getTicketsUseCase;
    private final ManageTicketUseCase manageTicketUseCase;

    // ── Dashboard ─────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "message", "Admin Dashboard",
                "status", "OK"));
    }

    // ── User Management ───────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                getAllUsersUseCase.getAllUsers(PageRequest.of(page, size)));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(getUserByIdUseCase.getUserById(id));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(
                updateUserUseCase.updateUser(
                        id,
                        request.getEmail(),
                        request.getTimezone(),
                        request.getActive()));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
        deactivateUserUseCase.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<User> assignRole(
            @PathVariable UUID id,
            @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(
                assignRoleUseCase.assignRole(id, request.getRoleCode()));
    }

    // ── Session Management ────────────────────────────────────

    @DeleteMapping("/users/{userId}/revoke")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Void> revokeUserSessions(
            @PathVariable UUID userId) {
        revokeAllSessionsUseCase.revokeAllSessions(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/{jti}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Void> revokeSession(
            @PathVariable String jti) {
        revokeSessionUseCase.revokeSession(jti);
        return ResponseEntity.noContent().build();
    }

    // ── Ticket Management (Admin) ─────────────────────────────

    @GetMapping("/tickets")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Page<SupportTicket>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        if (status != null) {
            return ResponseEntity.ok(
                    getTicketsUseCase.getTicketsByStatus(
                            status, PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(
                getTicketsUseCase.getAllTickets(PageRequest.of(page, size)));
    }

    @PutMapping("/tickets/{id}/assign")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<SupportTicket> assignTicket(
            @PathVariable UUID id,
            @RequestParam UUID staffId) {
        return ResponseEntity.ok(
                manageTicketUseCase.assignTicket(id, staffId));
    }

    @PutMapping("/tickets/{id}/status")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<SupportTicket> updateTicketStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(
                manageTicketUseCase.updateTicketStatus(id, status));
    }

    @PutMapping("/users/{id}/password")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<Map<String, String>> adminResetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        adminResetPasswordUseCase.adminResetPassword(id, request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

}
