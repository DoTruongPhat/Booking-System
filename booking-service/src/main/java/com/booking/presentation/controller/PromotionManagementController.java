package com.booking.presentation.controller;

import com.booking.application.service.PromotionManagementService;
import com.booking.presentation.request.PromotionRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.PromotionResponse;
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
public class PromotionManagementController {

    private final PromotionManagementService service;

    @GetMapping("/api/admin/promotions")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> findForAdmin(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.findForAdmin(hotelId, active, pageable)));
    }

    @PostMapping("/api/admin/promotions")
    public ResponseEntity<ApiResponse<PromotionResponse>> createForAdmin(
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForAdmin(request)));
    }

    @PutMapping("/api/admin/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updateForAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateForAdmin(id, request)));
    }

    @DeleteMapping("/api/admin/promotions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForAdmin(@PathVariable UUID id) {
        service.deleteForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted", null));
    }

    @GetMapping("/api/host/promotions")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> findForHost(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                service.findForHost(SecurityUtils.getCurrentUserId(), active, pageable)));
    }

    @PostMapping("/api/host/promotions")
    public ResponseEntity<ApiResponse<PromotionResponse>> createForHost(
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForHost(SecurityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/api/host/promotions/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updateForHost(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateForHost(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @DeleteMapping("/api/host/promotions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForHost(@PathVariable UUID id) {
        service.deleteForHost(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted", null));
    }
}
