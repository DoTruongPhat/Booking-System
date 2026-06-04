package com.booking.presentation.controller;

import com.booking.application.port.in.VerifyUserUseCase;
import com.booking.infrastructure.config.AppProperties;
import com.booking.presentation.request.VerifyUserRequest;
import com.booking.presentation.response.VerifyUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * InternalController = API nội bộ cho Keycloak
 *
 * Tại sao tách controller riêng?
 * → Endpoint này chỉ dành cho Keycloak gọi
 * → Không phải public API cho client
 * → Bảo vệ bằng X-Internal-Key header
 *   (chỉ Keycloak biết key này)
 *
 * Luồng Remote User Federation:
 * User login vào Keycloak
 *     ↓
 * Keycloak gọi POST /internal/users/verify
 *     ↓
 * Auth Service verify password trong DB
 *     ↓
 * Trả về valid: true/false + user info
 *     ↓
 * Keycloak tạo KC session
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@Log4j2
public class InternalController {
    private final VerifyUserUseCase verifyUserUseCase;
    private final AppProperties appProperties;

    /**
     * POST /api/internal/users/verify
     * → Keycloak gọi API này để verify username/password
     * → Bảo vệ bằng X-Internal-Key header
     */
    @PostMapping("/users/verify")
    public ResponseEntity<VerifyUserResponse> verifyUser(
            @Valid @RequestBody VerifyUserRequest request,
            @RequestHeader("X-Internal-Key") String internalKey) {
        log.info("[Internal] Verify user request: {}",
                request.getUsername());

        // → So sánh với config APP_INTERNAL_KEY
        // → Nếu sai → 401
        String expectedKey = appProperties
                .getSecurity()
                .getInternalKey();

        if(!expectedKey.equals(internalKey)) {
            log.warn("[Internal] Invalid internal key from request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        VerifyUserResponse response = verifyUserUseCase
                .verifyUser(request);

        return ResponseEntity.ok(response);

    }
}
