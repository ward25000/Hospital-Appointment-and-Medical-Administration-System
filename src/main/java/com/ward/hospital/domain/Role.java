package com.ward.hospital.domain;

public enum Role {
    ADMIN("Администратор"),
    DOCTOR("Врач"),
    RECEPTIONIST("Регистратор");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
