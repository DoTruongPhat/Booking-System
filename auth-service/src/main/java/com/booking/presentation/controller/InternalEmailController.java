package com.booking.presentation.controller;

import com.booking.application.service.MailService;
import com.booking.presentation.request.HotelDecisionEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/emails")
@RequiredArgsConstructor
public class InternalEmailController {

    private static final String INTERNAL_HEADER = "X-Internal-Api-Key";

    private final MailService mailService;

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @PostMapping("/hotel-decision")
    public ResponseEntity<Map<String, Object>> sendHotelDecision(
            @RequestHeader(value = INTERNAL_HEADER, required = false) String apiKey,
            @Valid @RequestBody HotelDecisionEmailRequest request) {

        if (!internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal API key");
        }

        if (!"APPROVED".equals(request.getDecision()) && !"REJECTED".equals(request.getDecision())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "decision must be APPROVED or REJECTED");
        }

        mailService.sendHotelDecision(
                request.getTo(),
                request.getHotelName(),
                request.getDecision(),
                request.getComment()
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hotel decision email sent"
        ));
    }
}
