package com.ward.hospital.util;

import com.ward.hospital.i18n.I18n;

/** Structured postal address: city, street, house and an optional apartment. */
public record Address(String city, String street, String house, String apartment) {

    public static Address of(String city, String street, String house, String apartment) {
        return new Address(trim(city), trim(street), trim(house), trim(apartment));
    }

    public boolean isComplete() {
        return city != null && street != null && house != null;
    }

    public String format(String lang) {
        String base = city + ", " + street + ", " + house;
        return apartment == null ? base : base + ", " + I18n.translate(lang, "кв. {0}", apartment);
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
