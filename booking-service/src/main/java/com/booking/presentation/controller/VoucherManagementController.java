package com.booking.presentation.controller;

import com.booking.application.service.VoucherManagementService;
import com.booking.presentation.request.VoucherRequest;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.VoucherResponse;
import com.booking.presentation.response.VoucherValidationResponse;
import com.booking.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class VoucherManagementController {

    private final VoucherManagementService service;

    @GetMapping("/api/vouchers/validate")
    public ResponseEntity<ApiResponse<VoucherValidationResponse>> validate(
            @RequestParam String code,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(service.validate(code, hotelId, amount)));
    }

    @GetMapping("/api/admin/vouchers")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> findForAdmin(
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.findForAdmin(hotelId, active, pageable)));
    }

    @PostMapping("/api/admin/vouchers")
    public ResponseEntity<ApiResponse<VoucherResponse>> createForAdmin(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForAdmin(request)));
    }

    @PutMapping("/api/admin/vouchers/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateForAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateForAdmin(id, request)));
    }

    @DeleteMapping("/api/admin/vouchers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForAdmin(@PathVariable UUID id) {
        service.deleteForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("Voucher deleted", null));
    }

    @GetMapping("/api/host/vouchers")
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> findForHost(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                service.findForHost(SecurityUtils.getCurrentUserId(), active, pageable)));
    }

    @PostMapping("/api/host/vouchers")
    public ResponseEntity<ApiResponse<VoucherResponse>> createForHost(
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createForHost(SecurityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/api/host/vouchers/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateForHost(
            @PathVariable UUID id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateForHost(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @DeleteMapping("/api/host/vouchers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForHost(@PathVariable UUID id) {
        service.deleteForHost(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Voucher deleted", null));
    }
}
