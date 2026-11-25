package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum PaymentStatus {
    UNPAID("Не оплачено"),
    PAID("Оплачено");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return I18n.t(label);
    }
}
