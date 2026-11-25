package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum ReceiptEmailStatus {
    PENDING("Отправляется"),
    SENT("Отправлена на почту"),
    SAVED("Сохранена на сервере (почта не настроена)"),
    FAILED("Не удалось отправить"),
    NO_EMAIL("Email не указан");

    private final String label;

    ReceiptEmailStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return I18n.t(label);
    }
}
