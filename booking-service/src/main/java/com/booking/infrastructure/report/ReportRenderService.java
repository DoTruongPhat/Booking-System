package com.booking.infrastructure.report;

import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.application.port.out.HotelRepositoryPort;
import com.booking.application.port.out.RoomRepositoryPort;
import com.booking.domain.model.Booking;
import com.booking.domain.model.Hotel;
import com.booking.domain.model.Room;
import com.booking.domain.model.report.ExportFormat;
import com.booking.domain.model.report.ReportTemplate;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.persistence.EntityNotFoundException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportRenderService {

    private static final Logger log = LoggerFactory.getLogger(ReportRenderService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final Map<String, JasperReport> reportCache;
    private final BookingRepositoryPort bookingRepository;
    private final RoomRepositoryPort roomRepository;
    private final HotelRepositoryPort hotelRepository;
    private final Locale defaultLocale;
    private final DataSource dataSource;

    public ReportRenderService(
            Map<String, JasperReport> jasperReportCache,
            BookingRepositoryPort bookingRepository,
            RoomRepositoryPort roomRepository,
            HotelRepositoryPort hotelRepository,
            Locale reportDefaultLocale,
            DataSource dataSource) {
        this.reportCache = jasperReportCache;
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.defaultLocale = reportDefaultLocale;
        this.dataSource = dataSource;
    }

    // ══════════════════════════════════════════════
    // 1. BOOKING CONFIRMATION PDF
    // ══════════════════════════════════════════════

    public byte[] renderBookingConfirmation(UUID bookingId, Locale locale) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        Locale effectiveLocale = locale != null ? locale : defaultLocale;
        JasperReport report = getCompiledReport(ReportTemplate.BOOKING_CONFIRMATION);
        Map<String, Object> params = buildConfirmationParams(booking, effectiveLocale);

        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            log.error("PDF render failed for booking {}", bookingId, e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private Map<String, Object> buildConfirmationParams(Booking booking, Locale locale) {
        Map<String, Object> p = new HashMap<>();
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(locale);

        // Booking info
        p.put("bookingCode", booking.getBookingCode());
        p.put("createdAt", booking.getCreatedAt() != null
                ? DATETIME_FMT.format(booking.getCreatedAt()) : "");
        p.put("status", booking.getStatus().name());

        // Guest info (snapshot)
        p.put("guestName", booking.getGuestName());
        p.put("guestEmail", booking.getGuestEmail());
        p.put("guestPhone", booking.getGuestPhone());

        // Room + Hotel info (load from DB)
        Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
        Hotel hotel = hotelRepository.findById(booking.getHotelId()).orElse(null);

        p.put("roomType", room != null && room.getRoomType() != null ? room.getRoomType() : "");
        p.put("roomName", room != null ? room.getName() : "");
        p.put("hotelName", hotel != null ? hotel.getName() : "");
        p.put("hotelAddress", hotel != null ? hotel.getAddress() : "");

        // Stay info
        p.put("checkIn", booking.getCheckInDate() != null
                ? booking.getCheckInDate().format(DATE_FMT) : "");
        p.put("checkOut", booking.getCheckOutDate() != null
                ? booking.getCheckOutDate().format(DATE_FMT) : "");
        p.put("nights", booking.getNumNights());
        p.put("adults", booking.getNumGuests());
        p.put("children", 0);

        // Payment — tính subtotal và tax từ domain fields
        BigDecimal unitPrice = booking.getUnitPrice() != null ? booking.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(
                booking.getNumNights() != null ? booking.getNumNights() : 1));
        BigDecimal tax = totalPrice.subtract(subtotal.subtract(discount))
                .max(BigDecimal.ZERO);

        p.put("pricePerNight", currFmt.format(unitPrice));
        p.put("subtotal", currFmt.format(subtotal));
        p.put("discount", discount.compareTo(BigDecimal.ZERO) > 0
                ? "-" + currFmt.format(discount) : currFmt.format(BigDecimal.ZERO));
        p.put("tax", currFmt.format(tax));
        p.put("total", currFmt.format(totalPrice));

        // QR code + Logo
        p.put("qrCodeImage", generateQRCode(booking.getId().toString()));
        p.put("logoImage", loadLogo());

        // Watermark
        p.put("showCancelledWatermark", "CANCELLED".equals(booking.getStatus().name()));

        // i18n
        p.put(JRParameter.REPORT_LOCALE, locale);
        p.put(JRParameter.REPORT_RESOURCE_BUNDLE,
                ResourceBundle.getBundle("jasper.templates.booking-confirmation", locale));

        return p;
    }

    // ══════════════════════════════════════════════
    // 2. ADMIN EXPORT BOOKINGS (XLSX/CSV)
    // ══════════════════════════════════════════════

    public byte[] exportBookings(BookingExportFilter filter, ExportFormat format) {
        List<Booking> bookings = bookingRepository.findByFilter(filter);
        log.info("Exporting {} bookings as {}", bookings.size(), format);

        List<Map<String, Object>> rows = bookings.stream()
                .map(this::bookingToExportRow).toList();

        try {
            JasperReport report = buildTabularReport();
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(rows);
            JasperPrint print = JasperFillManager.fillReport(report, Collections.emptyMap(), ds);

            return switch (format) {
                case PDF -> JasperExportManager.exportReportToPdf(print);
                case XLSX -> exportToXlsx(print);
                case CSV -> exportToCsv(print);
            };
        } catch (JRException e) {
            log.error("Export failed", e);
            throw new RuntimeException("Export failed", e);
        }
    }

    private Map<String, Object> bookingToExportRow(Booking b) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("bookingCode", b.getBookingCode());
        row.put("status", b.getStatus().name());
        row.put("guestName", b.getGuestName() != null ? b.getGuestName() : "");
        row.put("guestEmail", b.getGuestEmail() != null ? b.getGuestEmail() : "");

        // Load hotel name
        String hotelName = "";
        if (b.getHotelId() != null) {
            hotelName = hotelRepository.findById(b.getHotelId())
                    .map(Hotel::getName).orElse("");
        }
        row.put("hotelName", hotelName);

        // Load room name
        String roomName = "";
        if (b.getRoomId() != null) {
            roomName = roomRepository.findById(b.getRoomId())
                    .map(Room::getName).orElse("");
        }
        row.put("roomName", roomName);

        row.put("checkIn", b.getCheckInDate() != null ? b.getCheckInDate().format(DATE_FMT) : "");
        row.put("checkOut", b.getCheckOutDate() != null ? b.getCheckOutDate().format(DATE_FMT) : "");
        row.put("nights", b.getNumNights());
        row.put("totalPrice", b.getTotalPrice());
        row.put("createdAt", b.getCreatedAt() != null ? DATETIME_FMT.format(b.getCreatedAt()) : "");
        return row;
    }

    private JasperReport buildTabularReport() throws JRException {
        String jrxml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports
                              http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
                          name="bookings-export" pageWidth="842" pageHeight="595"
                          orientation="Landscape" columnWidth="802"
                          leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20">
                <field name="bookingCode" class="java.lang.String"/>
                <field name="status" class="java.lang.String"/>
                <field name="guestName" class="java.lang.String"/>
                <field name="guestEmail" class="java.lang.String"/>
                <field name="hotelName" class="java.lang.String"/>
                <field name="roomName" class="java.lang.String"/>
                <field name="checkIn" class="java.lang.String"/>
                <field name="checkOut" class="java.lang.String"/>
                <field name="nights" class="java.lang.Integer"/>
                <field name="totalPrice" class="java.math.BigDecimal"/>
                <field name="createdAt" class="java.lang.String"/>
                <columnHeader>
                    <band height="20">
                        <staticText><reportElement x="0" y="0" width="75" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Mã]]></text></staticText>
                        <staticText><reportElement x="75" y="0" width="60" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Trạng thái]]></text></staticText>
                        <staticText><reportElement x="135" y="0" width="100" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Khách]]></text></staticText>
                        <staticText><reportElement x="235" y="0" width="120" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Email]]></text></staticText>
                        <staticText><reportElement x="355" y="0" width="100" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Khách sạn]]></text></staticText>
                        <staticText><reportElement x="455" y="0" width="70" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Phòng]]></text></staticText>
                        <staticText><reportElement x="525" y="0" width="65" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Check-in]]></text></staticText>
                        <staticText><reportElement x="590" y="0" width="65" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Check-out]]></text></staticText>
                        <staticText><reportElement x="655" y="0" width="40" height="20"/>
                            <textElement><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Đêm]]></text></staticText>
                        <staticText><reportElement x="695" y="0" width="107" height="20"/>
                            <textElement textAlignment="Right"><font size="8" isBold="true"/></textElement>
                            <text><![CDATA[Tổng tiền]]></text></staticText>
                    </band>
                </columnHeader>
                <detail>
                    <band height="16">
                        <textField isBlankWhenNull="true"><reportElement x="0" y="0" width="75" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{bookingCode}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="75" y="0" width="60" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{status}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="135" y="0" width="100" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{guestName}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="235" y="0" width="120" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{guestEmail}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="355" y="0" width="100" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{hotelName}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="455" y="0" width="70" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{roomName}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="525" y="0" width="65" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{checkIn}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="590" y="0" width="65" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{checkOut}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="655" y="0" width="40" height="16"/>
                            <textElement><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{nights}]]></textFieldExpression></textField>
                        <textField isBlankWhenNull="true"><reportElement x="695" y="0" width="107" height="16"/>
                            <textElement textAlignment="Right"><font size="7"/></textElement>
                            <textFieldExpression><![CDATA[$F{totalPrice}]]></textFieldExpression></textField>
                    </band>
                </detail>
            </jasperReport>
            """;
        return JasperCompileManager.compileReport(new ByteArrayInputStream(jrxml.getBytes()));
    }

    // ══════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════

    private JasperReport getCompiledReport(ReportTemplate template) {
        JasperReport report = reportCache.get(template.getTemplateName());
        if (report == null) {
            throw new IllegalStateException("Template not in cache: " + template.getTemplateName());
        }
        return report;
    }

    private InputStream generateQRCode(String content) {
        try {
            var matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (Exception e) {
            log.warn("QR code generation failed: {}", content, e);
            return null;
        }
    }

    private InputStream loadLogo() {
        for (String path : List.of("jasper/images/logo.png", "jasper/images/Logo.jpg")) {
            try {
                ClassPathResource res = new ClassPathResource(path);
                if (res.exists()) {
                    return res.getInputStream();
                }
            } catch (IOException e) {
                log.warn("Logo load failed for {}", path);
            }
        }
        log.warn("Logo not found in Jasper resources");
        return null;
    }

    private byte[] exportToXlsx(JasperPrint print) throws JRException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));
        SimpleXlsxReportConfiguration cfg = new SimpleXlsxReportConfiguration();
        cfg.setOnePagePerSheet(false);
        cfg.setRemoveEmptySpaceBetweenRows(true);
        cfg.setDetectCellType(true);
        exporter.setConfiguration(cfg);
        exporter.exportReport();
        return baos.toByteArray();
    }

    private byte[] exportToCsv(JasperPrint print) throws JRException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JRCsvExporter exporter = new JRCsvExporter();
        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.setExporterOutput(new SimpleWriterExporterOutput(baos, "UTF-8"));
        exporter.exportReport();
        return baos.toByteArray();
    }

    public byte[] renderRevenueReport(LocalDate from, LocalDate to, ExportFormat format) {
        return renderRevenueReport(from, to, format, null, null, "REVENUE REPORT");
    }

    public byte[] renderRevenueReport(LocalDate from, LocalDate to, ExportFormat format, UUID hotelId) {
        return renderRevenueReport(from, to, format, null, hotelId, "REVENUE REPORT");
    }

    public byte[] renderHostRevenueReport(LocalDate from, LocalDate to, ExportFormat format, UUID ownerUserId) {
        return renderRevenueReport(from, to, format, ownerUserId, null, "HOST REVENUE REPORT");
    }

    public byte[] renderHostRevenueReport(
            LocalDate from,
            LocalDate to,
            ExportFormat format,
            UUID ownerUserId,
            UUID hotelId) {
        return renderRevenueReport(from, to, format, ownerUserId, hotelId, "HOST REVENUE REPORT");
    }

    public byte[] renderMonthlyRevenueReport(
            int year,
            int month,
            ExportFormat format,
            UUID ownerUserId,
            UUID hotelId) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String title = ownerUserId == null ? "MONTHLY REVENUE REPORT" : "HOST MONTHLY REVENUE REPORT";
        return renderSqlReport(
                ReportTemplate.MONTHLY_REVENUE,
                from,
                to,
                format,
                ownerUserId,
                hotelId,
                title,
                Collections.emptyMap()
        );
    }

    public byte[] renderCommissionReport(
            LocalDate from,
            LocalDate to,
            ExportFormat format,
            UUID ownerUserId,
            UUID hotelId,
            BigDecimal commissionRate) {
        String title = ownerUserId == null ? "COMMISSION REPORT" : "HOST COMMISSION REPORT";
        return renderSqlReport(
                ReportTemplate.COMMISSION_REPORT,
                from,
                to,
                format,
                ownerUserId,
                hotelId,
                title,
                Map.of("commissionRate", normalizeCommissionRate(commissionRate))
        );
    }

    private byte[] renderRevenueReport(
            LocalDate from,
            LocalDate to,
            ExportFormat format,
            UUID ownerUserId,
            UUID hotelId,
            String reportTitle) {
        return renderSqlReport(
                ReportTemplate.REVENUE_REPORT,
                from,
                to,
                format,
                ownerUserId,
                hotelId,
                reportTitle,
                Collections.emptyMap()
        );
    }

    private byte[] renderSqlReport(
            ReportTemplate template,
            LocalDate from,
            LocalDate to,
            ExportFormat format,
            UUID ownerUserId,
            UUID hotelId,
            String reportTitle,
            Map<String, Object> extraParams) {
        JasperReport report = getCompiledReport(template);

        Map<String, Object> params = new HashMap<>(extraParams);
        params.put("fromDate", java.sql.Date.valueOf(from));
        params.put("toDate", java.sql.Date.valueOf(to));
        params.put("ownerUserId", ownerUserId != null ? ownerUserId.toString() : null);
        params.put("hotelId", hotelId != null ? hotelId.toString() : null);
        params.put("reportTitle", reportTitle);

        try (Connection connection = dataSource.getConnection()) {
            // Key difference: pass JDBC Connection, NOT Java data
            // Jasper executes SQL in .jrxml directly against PostgreSQL
            JasperPrint print = JasperFillManager.fillReport(report, params, connection);

            return switch (format) {
                case PDF -> JasperExportManager.exportReportToPdf(print);
                case XLSX -> exportToXlsx(print);
                case CSV -> exportToCsv(print);
            };
        } catch (Exception e) {
            log.error("{} failed: from={}, to={}, owner={}, hotel={}", template, from, to, ownerUserId, hotelId, e);
            throw new RuntimeException(template.getTemplateName() + " generation failed", e);
        }
    }

    private BigDecimal normalizeCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null) {
            return new BigDecimal("0.10");
        }
        if (commissionRate.compareTo(BigDecimal.ZERO) < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Commission rate must be between 0 and 1");
        }
        return commissionRate;
    }

    public byte[] renderPaymentReceipt(UUID bookingId, Locale locale) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        Locale effectiveLocale = locale != null ? locale : defaultLocale;
        JasperReport report = getCompiledReport(ReportTemplate.PAYMENT_RECEIPT);
        Map<String, Object> params = buildReceiptParams(booking, effectiveLocale);

        try {
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(print);
        } catch (JRException e) {
            log.error("Receipt render failed for booking {}", bookingId, e);
            throw new RuntimeException("Receipt generation failed", e);
        }
    }

    private Map<String, Object> buildReceiptParams(Booking booking, Locale locale) {
        Map<String, Object> p = new HashMap<>();
        NumberFormat currFmt = NumberFormat.getCurrencyInstance(locale);

        // Receipt info
        p.put("receiptNumber", "RCP-" + booking.getBookingCode());
        p.put("bookingCode", booking.getBookingCode());
        p.put("createdAt", booking.getCreatedAt() != null
                ? DATETIME_FMT.format(booking.getCreatedAt()) : "");
        p.put("paidAt", booking.getPaidAt() != null
                ? DATETIME_FMT.format(booking.getPaidAt()) : DATETIME_FMT.format(booking.getCreatedAt()));
        p.put("paymentMethod", booking.getPaymentMethod() != null
                ? booking.getPaymentMethod() : "N/A");
        p.put("paymentStatus", booking.getPaymentStatus() != null
                ? booking.getPaymentStatus().name() : "UNPAID");

        // Guest info
        p.put("guestName", booking.getGuestName());
        p.put("guestEmail", booking.getGuestEmail());
        p.put("guestPhone", booking.getGuestPhone());

        // Room + Hotel
        Room room = roomRepository.findById(booking.getRoomId()).orElse(null);
        Hotel hotel = hotelRepository.findById(booking.getHotelId()).orElse(null);

        p.put("hotelName", hotel != null ? hotel.getName() : "");
        p.put("hotelAddress", hotel != null ? hotel.getAddress() : "");
        p.put("roomType", room != null && room.getRoomType() != null ? room.getRoomType() : "");
        p.put("roomName", room != null ? room.getName() : "");

        // Stay info
        p.put("checkIn", booking.getCheckInDate() != null
                ? booking.getCheckInDate().format(DATE_FMT) : "");
        p.put("checkOut", booking.getCheckOutDate() != null
                ? booking.getCheckOutDate().format(DATE_FMT) : "");
        p.put("nights", booking.getNumNights());
        p.put("numRooms", booking.getNumRooms());

        // Payment breakdown
        BigDecimal unitPrice = booking.getUnitPrice() != null ? booking.getUnitPrice() : BigDecimal.ZERO;
        BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalPrice = booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO;

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(
                booking.getNumNights() != null ? booking.getNumNights() : 1));
        BigDecimal tax = totalPrice.subtract(subtotal.subtract(discount)).max(BigDecimal.ZERO);

        p.put("unitPrice", currFmt.format(unitPrice));
        p.put("subtotal", currFmt.format(subtotal));
        p.put("discount", discount.compareTo(BigDecimal.ZERO) > 0
                ? "-" + currFmt.format(discount) : currFmt.format(BigDecimal.ZERO));
        p.put("tax", currFmt.format(tax));
        p.put("totalPaid", currFmt.format(totalPrice));

        // Logo
        p.put("logoImage", loadLogo());

        return p;
    }

}
