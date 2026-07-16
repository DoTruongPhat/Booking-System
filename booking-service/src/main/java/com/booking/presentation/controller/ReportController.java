package com.booking.presentation.controller;

import com.booking.domain.model.report.ExportFormat;
import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.infrastructure.report.BookingExportFilter;
import com.booking.infrastructure.report.ReportRenderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportRenderService reportRenderService;
    private final BookingRepositoryPort bookingRepository;

    public ReportController(ReportRenderService reportRenderService,
                            BookingRepositoryPort bookingRepository) {
        this.reportRenderService = reportRenderService;
        this.bookingRepository = bookingRepository;
    }

    // GET /api/user/bookings/{id}/confirmation.pdf
    @GetMapping("/user/bookings/{bookingId}/confirmation.pdf")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadConfirmation(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Ownership check
        boolean isOwner = userId.equals(booking.getUserId().toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }

        // PENDING → 400
        if ("PENDING".equals(booking.getStatus().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot generate confirmation for PENDING booking");
        }

        byte[] pdf = reportRenderService.renderBookingConfirmation(bookingId, Locale.forLanguageTag(lang));
        String filename = "booking-" + booking.getBookingCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // GET /api/admin/bookings/export?format=xlsx&from=&to=&hotelId=
    @GetMapping("/admin/bookings/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    public ResponseEntity<byte[]> exportBookings(
            @RequestParam(defaultValue = "XLSX") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) String status) {

        ExportFormat exportFormat;
        try {
            exportFormat = ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format: PDF, XLSX, CSV");
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        BookingExportFilter filter = BookingExportFilter.of(from, to, hotelId, status);
        byte[] data = reportRenderService.exportBookings(filter, exportFormat);
        String filename = "bookings-" + from + "-to-" + to + exportFormat.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/admin/reports/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> revenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "PDF") String format) {

        ExportFormat exportFormat;
        try {
            exportFormat = ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format: PDF, XLSX, CSV");
        }

        byte[] data = reportRenderService.renderRevenueReport(from, to, exportFormat);

        String filename = "revenue-report-" + from + "-to-" + to + exportFormat.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    // GET /api/user/bookings/{id}/receipt.pdf
    @GetMapping("/user/bookings/{bookingId}/receipt.pdf")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Ownership check
        boolean isOwner = userId.equals(booking.getUserId().toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }

        // Only CONFIRMED or COMPLETED can have receipt
        String status = booking.getStatus().name();
        if (!"CONFIRMED".equals(status) && !"COMPLETED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Receipt only available for CONFIRMED or COMPLETED bookings");
        }

        byte[] pdf = reportRenderService.renderPaymentReceipt(bookingId, Locale.forLanguageTag(lang));
        String filename = "receipt-" + booking.getBookingCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}