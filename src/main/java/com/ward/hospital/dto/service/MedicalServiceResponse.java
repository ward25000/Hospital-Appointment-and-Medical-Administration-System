package com.ward.hospital.dto.service;

import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.i18n.I18n;

import java.math.BigDecimal;

public record MedicalServiceResponse(
        Long id,
        String name,
        String description,
        String nameRu,
        String nameKk,
        String nameEn,
        String descriptionRu,
        String descriptionKk,
        String descriptionEn,
        BigDecimal price,
        int durationMinutes,
        boolean diagnostic,
        String office,
        Long specializationId,
        String specializationName,
        boolean active
) {

    public static MedicalServiceResponse from(MedicalService service) {
        String lang = I18n.lang();
        return new MedicalServiceResponse(
                service.getId(),
                service.name(lang),
                service.description(lang),
                service.getName(),
                service.getNameKk(),
                service.getNameEn(),
                service.getDescription(),
                service.getDescriptionKk(),
                service.getDescriptionEn(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.isDiagnostic(),
                service.getOffice(),
                service.getSpecialization() == null ? null : service.getSpecialization().getId(),
                service.getSpecialization() == null ? null : service.getSpecialization().name(lang),
                service.isActive()
        );
    }
}
