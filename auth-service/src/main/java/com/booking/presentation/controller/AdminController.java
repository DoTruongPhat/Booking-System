package com.booking.presentation.controller;

import com.booking.application.port.in.*;
import com.booking.application.port.out.RoleRepositoryPort;
import com.booking.application.port.out.TokenRepositoryPort;
import com.booking.application.port.out.UserRepositoryPort;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Log4j2
public class AdminController {

 // Session
 private final ManageSessionUseCase manageSessionUseCase;

 // User Management
 private final GetAllUsersUseCase getAllUsersUseCase;
 private final GetUserByIdUseCase getUserByIdUseCase;
 private final UpdateUserUseCase updateUserUseCase;
 private final DeactivateUserUseCase deactivateUserUseCase;
 private final AssignRoleUseCase assignRoleUseCase;
 private final AdminResetPasswordUseCase adminResetPasswordUseCase;
 private final RoleRepositoryPort roleRepositoryPort;

 // Ticket Management
 private final GetTicketsUseCase getTicketsUseCase;
 private final ManageTicketUseCase manageTicketUseCase;

 // Inject để count users cho dashboard
 private final UserRepositoryPort userRepositoryPort;

 private final TokenRepositoryPort tokenRepositoryPort;

 // ── Dashboard ─────────────────────────────────────────────
 @GetMapping("/dashboard")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN', 'HOST')")
 public ResponseEntity<Map<String, Object>> dashboard() {
 // Đếm users từ DB thật, không hard code
 // bookings/rooms/revenue = 0 vì booking-service, payment-service chưa có
 Map<String, Object> stats = new LinkedHashMap<>();
 stats.put("totalUsers", userRepositoryPort.count());
 stats.put("totalBookings", 0L);
 stats.put("totalRooms", 0L);
 stats.put("totalRevenue", 0L);
 stats.put("message", "Admin Dashboard");
 stats.put("status", "OK");
 return ResponseEntity.ok(stats);
 }

 // ── User Management ───────────────────────────────────────

 @GetMapping("/users")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<Page<User>> getUsers(
 @RequestParam(defaultValue = "0") int page,
 @RequestParam(defaultValue = "10") int size) {
 return ResponseEntity.ok(
 getAllUsersUseCase.getAllUsers(PageRequest.of(page, size)));
 }

 @GetMapping("/users/{id}")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<User> getUserById(@PathVariable UUID id) {
 return ResponseEntity.ok(getUserByIdUseCase.getUserById(id));
 }

 @PutMapping("/users/{id}")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
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
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<Void> deactivateUser(@PathVariable UUID id) {
 deactivateUserUseCase.deactivateUser(id);
 return ResponseEntity.noContent().build();
 }

 @PostMapping("/users/{id}/roles")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<User> assignRole(
 @PathVariable UUID id,
 @RequestBody AssignRoleRequest request) {
 return ResponseEntity.ok(
 assignRoleUseCase.assignRole(id, request.getRoleCode()));
 }

 // ── Session Management ────────────────────────────────────

 @DeleteMapping("/users/{userId}/revoke")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<Void> revokeUserSessions(
 @PathVariable UUID userId) {
 manageSessionUseCase.revokeAllSessions(userId);
 return ResponseEntity.noContent().build();
 }

 @DeleteMapping("/sessions/{jti}")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<Void> revokeSession(
 @PathVariable String jti) {
 manageSessionUseCase.revokeSession(jti);
 return ResponseEntity.noContent().build();
 }

 // ── Ticket Management (Admin) ─────────────────────────────

 @GetMapping("/tickets")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
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
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<SupportTicket> assignTicket(
 @PathVariable UUID id,
 @RequestParam UUID staffId) {
 return ResponseEntity.ok(
 manageTicketUseCase.assignTicket(id, staffId));
 }

 @PutMapping("/tickets/{id}/status")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<SupportTicket> updateTicketStatus(
 @PathVariable UUID id,
 @RequestParam String status) {
 return ResponseEntity.ok(
 manageTicketUseCase.updateTicketStatus(id, status));
 }

 @PutMapping("/users/{id}/password")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<Map<String, String>> adminResetPassword(
 @PathVariable UUID id,
 @Valid @RequestBody AdminResetPasswordRequest request) {
 adminResetPasswordUseCase.adminResetPassword(id, request.getNewPassword());
 return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
 }

 @GetMapping("/roles")
 @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
 public ResponseEntity<?> getAllRoles() {
  return ResponseEntity.ok(roleRepositoryPort.findAll());
 }

 @DeleteMapping("/users/{id}/hard")
 @PreAuthorize("hasAuthority('ADMIN_ALL')")
 public ResponseEntity<Void> hardDeleteUser(@PathVariable UUID id) {
  log.info("[Admin] Hard delete user: {}", id);
  // Xóa kc_links trước (FK)
  // Xóa tokens
  // Xóa user
  tokenRepositoryPort.deactivateAllByUserId(id, "ADMIN_DELETE");
  userRepositoryPort.deleteById(id);
  return ResponseEntity.noContent().build();
 }
}