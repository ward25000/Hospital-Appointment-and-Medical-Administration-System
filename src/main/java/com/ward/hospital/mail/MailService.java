package com.ward.hospital.mail;

import com.ward.hospital.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final String TIMEOUT_MS = "10000";

    private final MailProperties properties;
    private final JavaMailSenderImpl sender = new JavaMailSenderImpl();

    public MailService(MailProperties properties) {
        this.properties = properties;
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // Without these JavaMail resolves the computer name, which is slow and leaks it into the headers.
        Properties mail = sender.getJavaMailProperties();
        mail.put("mail.from", properties.from());
        mail.put("mail.smtp.localhost", properties.from().substring(properties.from().indexOf('@') + 1));
        if (properties.smtpConfigured()) {
            sender.setHost(properties.host());
            sender.setPort(properties.port());
            sender.setUsername(properties.username());
            sender.setPassword(properties.password());
            mail.put("mail.smtp.auth", String.valueOf(properties.username() != null && !properties.username().isBlank()));
            mail.put("mail.smtp.ssl.enable", String.valueOf(properties.ssl()));
            mail.put("mail.smtp.starttls.enable", String.valueOf(properties.starttls()));
            mail.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
            mail.put("mail.smtp.timeout", TIMEOUT_MS);
            mail.put("mail.smtp.writetimeout", TIMEOUT_MS);
        }
    }

    /**
     * @return true when the message was handed to the SMTP server, false when SMTP
     * is not configured and the message was stored in the outbox folder
     */
    public boolean send(OutgoingMail mail) throws MessagingException, IOException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.from());
        helper.setTo(mail.to());
        helper.setSubject(mail.subject());
        helper.setText(mail.html(), true);
        helper.addAttachment(mail.attachmentName(), new ByteArrayResource(mail.attachment()), "application/pdf");

        if (properties.smtpConfigured()) {
            sender.send(message);
            return true;
        }
        Path folder = Files.createDirectories(Path.of(properties.outboxDir()));
        Path file = folder.resolve(mail.attachmentName().replace(".pdf", "") + "-" + System.currentTimeMillis() + ".eml");
        try (OutputStream output = Files.newOutputStream(file)) {
            message.writeTo(output);
        }
        log.info("SMTP is not configured, the message was saved to {}", file.toAbsolutePath());
        return false;
    }
}
