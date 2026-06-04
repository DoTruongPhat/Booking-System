package com.booking.infrastructure.external.kafka;

import com.booking.application.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class EmailConsumer {
    private final JavaMailSender mailSender;

    private static final String TOPIC = "email-notifications";
    private static final String GROUP = "booking-group";

    @KafkaListener(topics = TOPIC, groupId = GROUP)
    public void consume(EmailEvent event) {
        log.info("[Kafka] Received email event: type={}",
                event.getType());

        try{
            switch (event.getType()) {
                case "FORGOT_PASSWORD" ->
                    sendForgotPasswordEmail(event);

                case "WELCOME" ->
                    sendWelcomeEmail(event);

                default ->
                        log.warn("[Kafka] Unknown event type: {}",
                                event.getType());
            }
        } catch (Exception e) {
            log.error("[Kafka] Failed to send email: {}",
                    e.getMessage());
        }
    }

    private void sendWelcomeEmail(EmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@booking.com");
        message.setTo(event.getTo());
        message.setSubject("Welcome to Booking System!");
        message.setText(
                "Hello " + event.getUsername() + ",\n\n" +
                "Welcome to Booking System!\n" +
                "Your account has been created successfully.\n\n" +
                "Booking System Team"
        );
        mailSender.send(message);
        log.info("[Email] Welcome email sent to: {}***",
                event.getTo().substring(0, 3));
    }

    private void sendForgotPasswordEmail(EmailEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@booking.com");
        message.setTo(event.getTo());
        message.setSubject("Reset Your Password - Booking System");
        message.setText(
                        "Hello " + event.getUsername() + ",\n\n" +
                        "You requested to reset your password.\n" +
                        "Click the link below to reset:\n\n" +
                        "http://localhost:4200/reset-password?token="
                        + event.getPayload() + "\n\n" +
                        "This link expires in 15 minutes.\n\n" +
                        "If you did not request this, ignore this email.\n\n" +
                        "Booking System Team"
        );
        mailSender.send(message);
        log.info("[Email] Forgot password email sent to: {}***",
                event.getTo().substring(0, 3));
    }

}
