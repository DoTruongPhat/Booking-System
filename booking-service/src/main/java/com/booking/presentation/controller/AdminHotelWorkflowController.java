package com.booking.presentation.controller;

import com.booking.infrastructure.persistence.repository.HotelWorkflowApprovalJpaRepository;
import com.booking.presentation.response.ApiResponse;
import com.booking.presentation.response.HotelWorkflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/hotel-workflows")
@RequiredArgsConstructor
public class AdminHotelWorkflowController {

    private final HotelWorkflowApprovalJpaRepository workflowRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<HotelWorkflowResponse>>> getWorkflows(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<HotelWorkflowResponse> workflows = workflowRepository.findAll(pageable)
                .map(HotelWorkflowResponse::from);
        return ResponseEntity.ok(ApiResponse.success(workflows));
    }
}
