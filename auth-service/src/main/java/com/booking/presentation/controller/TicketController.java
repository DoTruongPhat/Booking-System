package com.booking.presentation.controller;

import com.booking.application.port.in.CreateTicketUseCase;
import com.booking.application.port.in.GetProfileUseCase;
import com.booking.application.port.in.GetTicketsUseCase;
import com.booking.application.service.SupportTicketService;
import com.booking.domain.model.SupportTicket;
import com.booking.presentation.request.CreateTicketRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * TicketController = User tạo/xem tickets
 *
 * User tạo ticket → Admin assign → Staff xử lý → Close
 */

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Log4j2
public class TicketController {
    private final CreateTicketUseCase createTicketUseCase;
    private final GetTicketsUseCase getTicketsUseCase;
    private final GetProfileUseCase getProfileUseCase;

    @PostMapping
    public ResponseEntity<SupportTicket> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        SupportTicket ticket = createTicketUseCase.createTicket(
                getUserId(),
                request.getTitle(),
                request.getDescription(),
                request.getPriority());
        return ResponseEntity.ok(ticket);
    }

    @GetMapping
    public ResponseEntity<Page<SupportTicket>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                getTicketsUseCase.getMyTickets(
                        getUserId(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportTicket> getTicketById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                getTicketsUseCase.getTicketById(id));
    }

    private UUID getUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return getProfileUseCase.getProfile(username).getId();
    }

}
