package com.booking.infrastructure.external.kafka;

import com.booking.application.event.EmailEvent;
import com.booking.application.port.out.DomainEventPublisher;
import com.booking.domain.event.DomainEvent;
import com.booking.domain.event.PasswordResetRequestedEvent;
import com.booking.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * KafkaDomainEventPublisher = implements DomainEventPublisher
 *
 * Luồng:
 * AuthService publish DomainEvent
 *     ↓
 * KafkaDomainEventPublisher nhận
 *     ↓ convert sang EmailEvent
 * EmailProducer gửi Kafka
 *     ↓
 * EmailConsumer nhận → gửi email
 *
 * Tại sao nằm ở infrastructure?
 * → Biết Kafka tồn tại
 * → Biết EmailProducer
 * → Application không cần biết những thứ này
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final EmailProducer emailProducer;

    @Override
    public void publish(DomainEvent event) {
        // Pattern matching (Java 16+)
        // → Kiểm tra loại event → xử lý tương ứng
        // → Dễ thêm event mới mà không sửa logic cũ
        if (event instanceof PasswordResetRequestedEvent e) {
            handlePasswordResetRequested(e);
        } else if (event instanceof UserRegisteredEvent e) {
            handleUserRegistered(e);
        } else {
            log.warn("[Event] Unknown event type: {}",
                    event.getClass().getSimpleName());
        }
    }

    /**
     * User yêu cầu reset password
     * → Gửi email reset link
     */
    private void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        EmailEvent emailEvent = EmailEvent.builder()
                .type("FORGOT_PASSWORD")
                .to(event.getEmail())
                .username(event.getUsername())
                .payload(event.getResetToken())
                .build();
        emailProducer.sendEmail(emailEvent);
        log.info("[Event] PasswordResetRequestedEvent published for: {}",
                event.getUsername());
    }

    /**
     * User đăng ký thành công
     * → Gửi email chào mừng
     */
    private void handleUserRegistered(UserRegisteredEvent event) {
        EmailEvent emailEvent = EmailEvent.builder()
                .type("USER_REGISTERED")
                .to(event.getEmail())
                .username(event.getUsername())
                .payload(event.getUserId())
                .build();
        emailProducer.sendEmail(emailEvent);
        log.info("[Event] UserRegisteredEvent published for: {}",
                event.getUsername());
    }
}
