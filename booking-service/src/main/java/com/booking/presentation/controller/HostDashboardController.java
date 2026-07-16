package com.booking.presentation.controller;

import com.booking.application.port.in.HostDashboardUseCase;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.HostDashboardResponse;
import com.booking.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/host/dashboard")
@RequiredArgsConstructor
public class HostDashboardController {

    private final HostDashboardUseCase hostDashboardUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<HostDashboardResponse>> getDashboard() {
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        HostDashboardResponse dashboard = hostDashboardUseCase.getDashboard(ownerUserId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }
}
