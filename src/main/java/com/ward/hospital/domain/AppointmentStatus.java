package com.ward.hospital.domain;

public enum AppointmentStatus {
    SCHEDULED("Запланирован"),
    COMPLETED("Завершён"),
    CANCELLED("Отменён");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
