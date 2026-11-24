package com.ward.hospital.mail;

public record OutgoingMail(String to, String subject, String html, String attachmentName, byte[] attachment) {
}
