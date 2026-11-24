package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

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
        return I18n.t(label);
    }
}
