package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;

public enum Gender {
    MALE("Мужской"),
    FEMALE("Женский");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String label(String lang) {
        return I18n.translate(lang, label);
    }

    public String getLabel() {
        return I18n.t(label);
    }
}
