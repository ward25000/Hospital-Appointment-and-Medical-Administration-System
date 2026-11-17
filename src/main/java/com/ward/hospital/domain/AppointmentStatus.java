package com.ward.hospital.domain;

public enum AppointmentStatus {
    SCHEDULED("Ожидает"),
    COMPLETED("Приём проведён"),
    CANCELLED("Отменён"),
    NO_SHOW("Не явился");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
