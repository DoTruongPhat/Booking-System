package com.booking.infrastructure.report.consumer;

import com.booking.application.port.out.BookingRepositoryPort;
import com.booking.domain.event.BookingConfirmedEvent;
import com.booking.infrastructure.mail.MailService;
import com.booking.infrastructure.report.ReportRenderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

        // Idempotency check
        String key = PROCESSED_PREFIX + event.eventId();
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "1", PROCESSED_TTL);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("Event {} already processed — skip", event.eventId());
            return;
        }

        log.info("Processing: bookingId={}, code={}", event.bookingId(), event.bookingCode());

        try {
            // 1. Gen PDF
            byte[] pdf = reportRenderService.renderBookingConfirmation(
                    event.bookingId(), Locale.forLanguageTag("vi-VN"));
            log.info("PDF generated: {} bytes", pdf.length);

            // 2. Guest name từ snapshot
            String guestName = bookingRepository.findById(event.bookingId())
                    .map(b -> b.getGuestName() != null ? b.getGuestName() : "Quý khách")
                    .orElse("Quý khách");

            // 3. Send email
            String subject = "Xác nhận đặt phòng — " + event.bookingCode();
            String body = MailService.buildConfirmationEmailBody(event.bookingCode(), guestName);
            String filename = "booking-" + event.bookingCode() + ".pdf";

            mailService.sendWithAttachment(event.guestEmail(), subject, body, pdf, filename);
            log.info("Email queued for {}", event.guestEmail());

        } catch (Exception e) {
            log.error("Failed to process event {}", event.eventId(), e);
            redisTemplate.delete(key);
            throw e;
        }
    }
}