package com.booking.application.service.serviceimpl;

import com.booking.application.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtp(String to, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(getSubject(purpose));
            helper.setText(getBody(otp, purpose), true);  // true = HTML

            mailSender.send(message);
            log.info("[Mail] Sent OTP email to {} for {}", to, purpose);
        } catch (MessagingException e) {
            log.error("[Mail] Failed to send OTP to {}: {}", to, e.getMessage());
            throw new IllegalStateException("Failed to send OTP email", e);
        }
    }

    private String getSubject(String purpose) {
        return switch (purpose) {
            case "FORGOT_PASSWORD" -> "Reset your SmartBooking password";
            case "CHANGE_EMAIL" -> "Verify your new email";
            default -> "Your SmartBooking OTP code";
        };
    }

    private String getBody(String otp, String purpose) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>SmartBooking - %s</h2>
                <p>Your OTP code is:</p>
                <h1 style="color: #1890ff; letter-spacing: 5px;">%s</h1>
                <p>This code expires in 10 minutes.</p>
                <p>If you didn't request this, please ignore this email.</p>
            </body>
            </html>
            """.formatted(purpose, otp);
    }
}
