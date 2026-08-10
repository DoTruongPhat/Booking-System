package com.booking.presentation.controller;

import com.booking.application.service.RoomTypeManagementService;
import com.booking.presentation.request.RoomTypeRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.RoomTypeResponse;
import com.booking.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RoomTypeManagementController {

    private final RoomTypeManagementService service;

    @GetMapping("/api/admin/room-types")
    public ResponseEntity<ApiResponse<Page<RoomTypeResponse>>> findForAdmin(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.findForAdmin(hotelId, active, pageable)));
    }

    @PostMapping("/api/admin/room-types")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createForAdmin(
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForAdmin(request)));
    }

    @PutMapping("/api/admin/room-types/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateForAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateForAdmin(id, request)));
    }

    @DeleteMapping("/api/admin/room-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForAdmin(@PathVariable UUID id) {
        service.deleteForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Room type deleted", null));
    }

    @GetMapping("/api/host/room-types")
    public ResponseEntity<ApiResponse<Page<RoomTypeResponse>>> findForHost(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                service.findForHost(SecurityUtils.getCurrentUserId(), active, pageable)));
    }

    @PostMapping("/api/host/room-types")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createForHost(
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForHost(SecurityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/api/host/room-types/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateForHost(
            @PathVariable UUID id,
            @Valid @RequestBody RoomTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateForHost(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @DeleteMapping("/api/host/room-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForHost(@PathVariable UUID id) {
        service.deleteForHost(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Room type deleted", null));
    }
}
