package com.booking.presentation.controller;

import com.booking.application.service.SystemParamService;
import com.booking.domain.model.SystemParam;
import com.booking.presentation.request.UpdateSystemParamRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/system-params")
@Log4j2
public class SystemParamController {
    private final SystemParamService systemParamService;

    @GetMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<SystemParam> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(systemParamService.getByKey(key));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<List<SystemParam>> getAll() {
        return ResponseEntity.ok(systemParamService.getAll());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('ADMIN_ALL')")
    public ResponseEntity<SystemParam>
    update(@PathVariable String key,
           @RequestBody UpdateSystemParamRequest request,
           Authentication authentication) {

        String updatedBy = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return ResponseEntity.ok(
                systemParamService.update(key, request.getValue(), updatedBy));
    }


}
