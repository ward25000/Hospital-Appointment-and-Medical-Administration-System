package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum PaymentMethod {
    ONLINE_CARD("Онлайн, банковская карта"),
    CASH("Касса, наличные"),
    POS_CARD("Касса, банковская карта");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label(String lang) {
        return I18n.translate(lang, label);
    }

    public String getLabel() {
        return label(I18n.lang());
    }

    public boolean isCashDesk() {
        return this != ONLINE_CARD;
    }
}
