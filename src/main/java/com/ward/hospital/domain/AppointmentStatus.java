package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum AppointmentStatus {
    SCHEDULED("Запланирован"),
    COMPLETED("Прошел"),
    CANCELLED("Отменён"),
    NO_SHOW("Не явился");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return I18n.t(label);
    }
}
