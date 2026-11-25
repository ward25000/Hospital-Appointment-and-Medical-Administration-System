package com.ward.hospital.config;

import com.ward.hospital.i18n.I18n;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Requisites of the clinic printed on certificates and payment receipts. */
@ConfigurationProperties(prefix = "app.clinic")
public record ClinicProperties(
        String nameRu, String nameKk, String nameEn,
        String addressRu, String addressKk, String addressEn,
        String bin, String license, String phone, String email,
        String iik, String bik, String bankRu, String bankKk, String bankEn
) {

    public String name(String lang) {
        return I18n.pick(lang, nameRu, nameKk, nameEn);
    }

    public String address(String lang) {
        return I18n.pick(lang, addressRu, addressKk, addressEn);
    }

    public String bank(String lang) {
        return I18n.pick(lang, bankRu, bankKk, bankEn);
    }
}
