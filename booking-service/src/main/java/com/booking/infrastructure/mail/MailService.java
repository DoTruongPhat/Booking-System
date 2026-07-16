package com.booking.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@bookingsystem.vn}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendWithAttachment(String to, String subject, String htmlBody,
                                   byte[] attachment, String attachmentFilename) {
        if (!mailEnabled) {
            log.info("Mail disabled — skip send to {}", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment(attachmentFilename,
                    new ByteArrayResource(attachment), "application/pdf");
            mailSender.send(message);
            log.info("Email sent to {} with {}", to, attachmentFilename);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            // Fail-open: log lỗi, không throw
        }
    }

    public static String buildConfirmationEmailBody(String bookingCode, String guestName) {
        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2 style="color: #2196F3;">Xác nhận đặt phòng thành công!</h2>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Đặt phòng mã <strong>%s</strong> đã được xác nhận.</p>
                <p>Vui lòng xem chi tiết trong file PDF đính kèm.</p>
                <hr style="border: 1px solid #eee;"/>
                <p style="color: #999; font-size: 12px;">
                    Email tự động — không trả lời.<br/>
                    Hỗ trợ: support@bookingsystem.vn | 1900-xxxx
                </p>
            </body>
            </html>
            """.formatted(guestName, bookingCode);
    }
}