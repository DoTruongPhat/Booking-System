package com.booking.infrastructure.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public record MailAttachment(String filename, byte[] content, String contentType) {}

    public void sendWithAttachment(String to, String subject, String htmlBody,
                                   byte[] attachment, String attachmentFilename) {
        send(to, subject, htmlBody, List.of(
                new MailAttachment(attachmentFilename, attachment, "application/pdf")
        ));
    }

    public void sendWithAttachments(
            String to,
            String subject,
            String htmlBody,
            List<MailAttachment> attachments) {
        send(to, subject, htmlBody, attachments);
    }

    private void send(String to, String subject, String htmlBody, List<MailAttachment> attachments) {
        if (!mailEnabled) {
            log.info("Mail disabled - skip send to {}", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            for (MailAttachment attachment : attachments) {
                helper.addAttachment(
                        attachment.filename(),
                        new ByteArrayResource(attachment.content()),
                        attachment.contentType()
                );
            }

            mailSender.send(message);
            log.info("Email sent to {} with {} attachment(s)", to, attachments.size());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new IllegalStateException("Failed to send confirmation email", e);
        }
    }

    public static String buildConfirmationEmailBody(String bookingCode, String guestName) {
        return buildConfirmationEmailBody(bookingCode, guestName, false);
    }

    public static String buildConfirmationEmailBody(
            String bookingCode,
            String guestName,
            boolean includeReceipt) {
        String receiptLine = includeReceipt
                ? "<p>Bien nhan thanh toan duoc dinh kem cung email nay.</p>"
                : "";

        return """
            <html>
            <body style="font-family: Arial, sans-serif; color: #333;">
                <h2 style="color: #2196F3;">Xac nhan dat phong thanh cong!</h2>
                <p>Xin chao <strong>%s</strong>,</p>
                <p>Dat phong ma <strong>%s</strong> da duoc xac nhan.</p>
                <p>Vui long xem chi tiet trong file PDF dinh kem.</p>
                %s
                <hr style="border: 1px solid #eee;"/>
                <p style="color: #999; font-size: 12px;">
                    Email tu dong - khong tra loi.<br/>
                    Ho tro: support@bookingsystem.vn | 1900-xxxx
                </p>
            </body>
            </html>
            """.formatted(guestName, bookingCode, receiptLine);
    }
}
