package com.booking.infrastructure.report.consumer;

import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.domain.enums.PaymentStatus;
import com.booking.domain.event.BookingConfirmedEvent;
import com.booking.domain.model.Booking;
import com.booking.infrastructure.mail.MailService;
import com.booking.infrastructure.report.ReportRenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ReportGenerationConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationConsumer.class);
    private static final String PROCESSED_PREFIX = "report:processed:";
    private static final Duration PROCESSED_TTL = Duration.ofDays(7);

    private final ReportRenderService reportRenderService;
    private final MailService mailService;
    private final BookingRepositoryPort bookingRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ReportGenerationConsumer(
            ReportRenderService reportRenderService,
            MailService mailService,
            BookingRepositoryPort bookingRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.reportRenderService = reportRenderService;
        this.mailService = mailService;
        this.bookingRepository = bookingRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.booking-confirmed:booking-confirmed-events}",
            groupId = "${app.kafka.group.report:report-generation-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onBookingConfirmed(String message) {
        BookingConfirmedEvent event;
        try {
            event = objectMapper.readValue(message, BookingConfirmedEvent.class);
        } catch (Exception e) {
            log.error("Deserialize failed: {}", message, e);
            return;
        }

        String key = PROCESSED_PREFIX + event.eventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", PROCESSED_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Report event {} already processed - skip", event.eventId());
            return;
        }

        log.info("Processing report event: bookingId={}, code={}", event.bookingId(), event.bookingCode());

        try {
            Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);
            String guestName = booking != null && booking.getGuestName() != null
                    ? booking.getGuestName()
                    : "Quy khach";

            byte[] confirmationPdf = reportRenderService.renderBookingConfirmation(
                    event.bookingId(), Locale.forLanguageTag("vi-VN"));

            List<MailService.MailAttachment> attachments = new ArrayList<>();
            attachments.add(new MailService.MailAttachment(
                    "booking-" + event.bookingCode() + ".pdf",
                    confirmationPdf,
                    "application/pdf"
            ));
            log.info("Confirmation PDF generated: {} bytes", confirmationPdf.length);

            boolean includeReceipt = booking != null && booking.getPaymentStatus() == PaymentStatus.PAID;
            if (includeReceipt) {
                byte[] receiptPdf = reportRenderService.renderPaymentReceipt(
                        event.bookingId(), Locale.forLanguageTag("vi-VN"));
                attachments.add(new MailService.MailAttachment(
                        "receipt-" + event.bookingCode() + ".pdf",
                        receiptPdf,
                        "application/pdf"
                ));
                log.info("Payment receipt PDF generated: {} bytes", receiptPdf.length);
            }

            String subject = includeReceipt
                    ? "Xac nhan dat phong va bien nhan thanh toan - " + event.bookingCode()
                    : "Xac nhan dat phong - " + event.bookingCode();
            String body = MailService.buildConfirmationEmailBody(
                    event.bookingCode(), guestName, includeReceipt);

            mailService.sendWithAttachments(event.guestEmail(), subject, body, attachments);
            log.info("Email queued for {}", event.guestEmail());
        } catch (Exception e) {
            log.error("Failed to process report event {}", event.eventId(), e);
            redisTemplate.delete(key);
            throw e;
        }
    }
}
