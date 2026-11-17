package com.ward.hospital.domain;

public enum ReferralStatus {
    PENDING("Ожидает"),
    ACCEPTED("Принято"),
    COMPLETED("Выполнено"),
    CANCELLED("Отменено");

    private final String label;

    ReferralStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
