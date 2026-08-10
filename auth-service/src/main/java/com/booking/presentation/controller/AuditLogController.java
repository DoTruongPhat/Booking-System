package com.booking.presentation.controller;

import com.booking.application.service.AuditLogService;
import com.booking.presentation.response.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_ALL', 'ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String actorName,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.search(
                eventType,
                source,
                actorName,
                entityType,
                entityId,
                from,
                to,
                pageable));
    }
}
