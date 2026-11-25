package com.ward.hospital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP settings. When {@code host} is blank nothing is sent over the network:
 * the ready message is stored as an .eml file in {@code outboxDir}.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String host, int port, String username, String password,
        boolean ssl, boolean starttls, String from, String outboxDir
) {

    public boolean smtpConfigured() {
        return host != null && !host.isBlank();
    }
}
