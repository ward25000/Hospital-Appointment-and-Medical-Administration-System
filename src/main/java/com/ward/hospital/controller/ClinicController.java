package com.ward.hospital.controller;

import com.ward.hospital.config.ClinicProperties;
import com.ward.hospital.dto.clinic.ClinicResponse;
import com.ward.hospital.i18n.I18n;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clinic")
public class ClinicController {

    private final ClinicProperties clinic;

    public ClinicController(ClinicProperties clinic) {
        this.clinic = clinic;
    }

    @GetMapping
    public ClinicResponse clinic() {
        String lang = I18n.lang();
        return new ClinicResponse(clinic.name(lang), clinic.address(lang), clinic.phone(), clinic.email(),
                clinic.bin());
    }
}
