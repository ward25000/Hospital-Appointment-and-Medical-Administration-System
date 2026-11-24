package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum Role {
    ADMIN("Администратор"),
    DOCTOR("Врач"),
    RECEPTIONIST("Регистратор"),
    PATIENT("Пациент");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return I18n.t(label);
    }
}
