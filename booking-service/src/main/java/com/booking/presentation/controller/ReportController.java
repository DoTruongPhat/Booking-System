package com.booking.presentation.controller;

import com.booking.domain.model.report.ExportFormat;
import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.infrastructure.report.BookingExportFilter;
import com.booking.infrastructure.report.ReportRenderService;
import com.booking.shared.util.SecurityUtils;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportRenderService reportRenderService;
    private final BookingRepositoryPort bookingRepository;
    private final HotelRepositoryPort hotelRepository;

    public ReportController(ReportRenderService reportRenderService,
                            BookingRepositoryPort bookingRepository,
                            HotelRepositoryPort hotelRepository) {
        this.reportRenderService = reportRenderService;
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
    }

    // GET /api/user/bookings/{id}/confirmation.pdf
    @GetMapping("/user/bookings/{bookingId}/confirmation.pdf")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> downloadConfirmation(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = SecurityUtils.getCurrentUserId().toString();
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Ownership check
        boolean isOwner = userId.equals(booking.getUserId().toString());
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("ADMIN_ALL");
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> exportBookings(
            @RequestParam(defaultValue = "XLSX") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) String status) {

        ExportFormat exportFormat = parseExportFormat(format);

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

    // GET /api/host/bookings/export?format=xlsx&from=&to=&hotelId=
    @GetMapping("/host/bookings/export")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> exportHostBookings(
            @RequestParam(defaultValue = "XLSX") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(required = false) String status) {

        ExportFormat exportFormat = parseExportFormat(format);
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        if (hotelId != null) {
            assertHotelOwnedBy(hotelId, ownerUserId);
        }

        BookingExportFilter filter = BookingExportFilter.forOwner(from, to, hotelId, status, ownerUserId);
        byte[] data = reportRenderService.exportBookings(filter, exportFormat);
        String filename = "my-bookings-" + from + "-to-" + to + exportFormat.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/admin/reports/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> revenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "PDF") String format) {

        ExportFormat exportFormat = parseExportFormat(format);
        validateRange(from, to);

        byte[] data = reportRenderService.renderRevenueReport(from, to, exportFormat, hotelId);

        String filename = "revenue-report-" + from + "-to-" + to + exportFormat.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/host/reports/revenue")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> hostRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "PDF") String format) {

        ExportFormat exportFormat = parseExportFormat(format);
        validateRange(from, to);
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        if (hotelId != null) {
            assertHotelOwnedBy(hotelId, ownerUserId);
        }

        byte[] data = reportRenderService.renderHostRevenueReport(from, to, exportFormat, ownerUserId, hotelId);
        String filename = "host-revenue-report-" + from + "-to-" + to + exportFormat.getExtension();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    @GetMapping("/admin/reports/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> monthlyRevenueReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "PDF") String format) {

        validateMonth(year, month);
        ExportFormat exportFormat = parseExportFormat(format);
        byte[] data = reportRenderService.renderMonthlyRevenueReport(year, month, exportFormat, null, hotelId);
        String filename = "monthly-revenue-" + year + "-" + twoDigits(month) + exportFormat.getExtension();
        return download(data, filename, exportFormat);
    }

    @GetMapping("/host/reports/monthly-revenue")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> hostMonthlyRevenueReport(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "PDF") String format) {

        validateMonth(year, month);
        ExportFormat exportFormat = parseExportFormat(format);
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        if (hotelId != null) {
            assertHotelOwnedBy(hotelId, ownerUserId);
        }

        byte[] data = reportRenderService.renderMonthlyRevenueReport(year, month, exportFormat, ownerUserId, hotelId);
        String filename = "host-monthly-revenue-" + year + "-" + twoDigits(month) + exportFormat.getExtension();
        return download(data, filename, exportFormat);
    }

    @GetMapping("/admin/reports/commission")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> commissionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "0.10") BigDecimal commissionRate,
            @RequestParam(defaultValue = "PDF") String format) {

        ExportFormat exportFormat = parseExportFormat(format);
        validateRange(from, to);
        byte[] data = reportRenderService.renderCommissionReport(
                from, to, exportFormat, null, hotelId, validateCommissionRate(commissionRate));
        String filename = "commission-report-" + from + "-to-" + to + exportFormat.getExtension();
        return download(data, filename, exportFormat);
    }

    @GetMapping("/host/reports/commission")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> hostCommissionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID hotelId,
            @RequestParam(defaultValue = "0.10") BigDecimal commissionRate,
            @RequestParam(defaultValue = "PDF") String format) {

        ExportFormat exportFormat = parseExportFormat(format);
        validateRange(from, to);
        UUID ownerUserId = SecurityUtils.getCurrentUserId();
        if (hotelId != null) {
            assertHotelOwnedBy(hotelId, ownerUserId);
        }

        byte[] data = reportRenderService.renderCommissionReport(
                from, to, exportFormat, ownerUserId, hotelId, validateCommissionRate(commissionRate));
        String filename = "host-commission-report-" + from + "-to-" + to + exportFormat.getExtension();
        return download(data, filename, exportFormat);
    }

    @GetMapping("/host/bookings/{bookingId}/confirmation.pdf")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> downloadHostConfirmation(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang) {

        var booking = findHostBooking(bookingId, SecurityUtils.getCurrentUserId());
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

    @GetMapping("/host/bookings/{bookingId}/receipt.pdf")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<byte[]> downloadHostReceipt(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang) {

        var booking = findHostBooking(bookingId, SecurityUtils.getCurrentUserId());
        String status = booking.getStatus().name();
        if (!"CONFIRMED".equals(status) && !"CHECKED_IN".equals(status) && !"COMPLETED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Receipt only available for CONFIRMED, CHECKED_IN or COMPLETED bookings");
        }

        byte[] pdf = reportRenderService.renderPaymentReceipt(bookingId, Locale.forLanguageTag(lang));
        String filename = "receipt-" + booking.getBookingCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // GET /api/user/bookings/{id}/receipt.pdf
    @GetMapping("/user/bookings/{bookingId}/receipt.pdf")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ADMIN_ALL')")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable UUID bookingId,
            @RequestParam(required = false, defaultValue = "vi") String lang,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = SecurityUtils.getCurrentUserId().toString();
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        // Ownership check
        boolean isOwner = userId.equals(booking.getUserId().toString());
        boolean isAdmin = SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("ADMIN_ALL");
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }

        // Only CONFIRMED or COMPLETED can have receipt
        String status = booking.getStatus().name();
        if (!"CONFIRMED".equals(status) && !"CHECKED_IN".equals(status) && !"COMPLETED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Receipt only available for CONFIRMED, CHECKED_IN or COMPLETED bookings");
        }

        byte[] pdf = reportRenderService.renderPaymentReceipt(bookingId, Locale.forLanguageTag(lang));
        String filename = "receipt-" + booking.getBookingCode() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    private ExportFormat parseExportFormat(String format) {
        try {
            return ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid format: PDF, XLSX, CSV");
        }
    }

    private void validateMonth(int year, int month) {
        if (year < 2000 || year > 2100 || month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid year/month");
        }
    }

    private BigDecimal validateCommissionRate(BigDecimal commissionRate) {
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Commission rate must be between 0 and 1");
        }
        return commissionRate;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from/to are required");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to");
        }
    }

    private String twoDigits(int value) {
        return String.format("%02d", value);
    }

    private ResponseEntity<byte[]> download(byte[] data, String filename, ExportFormat exportFormat) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportFormat.getContentType()))
                .contentLength(data.length)
                .body(data);
    }

    private void assertHotelOwnedBy(UUID hotelId, UUID ownerUserId) {
        var hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hotel not found"));
        if (!hotel.isOwnedBy(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hotel is not owned by current host");
        }
    }

    private com.booking.domain.model.Booking findHostBooking(UUID bookingId, UUID ownerUserId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        assertHotelOwnedBy(booking.getHotelId(), ownerUserId);
        return booking;
    }
}
