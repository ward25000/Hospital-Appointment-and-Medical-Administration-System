package com.ward.hospital.dto.specialization;

import com.ward.hospital.domain.Specialization;
import com.ward.hospital.i18n.I18n;

public record SpecializationResponse(
        Long id,
        String name,
        String description,
        String nameRu,
        String nameKk,
        String nameEn,
        String descriptionRu,
        String descriptionKk,
        String descriptionEn,
        boolean active,
        long doctorsCount
) {

    public static SpecializationResponse from(Specialization specialization, long doctorsCount) {
        String lang = I18n.lang();
        return new SpecializationResponse(
                specialization.getId(),
                specialization.name(lang),
                specialization.description(lang),
                specialization.getName(),
                specialization.getNameKk(),
                specialization.getNameEn(),
                specialization.getDescription(),
                specialization.getDescriptionKk(),
                specialization.getDescriptionEn(),
                specialization.isActive(),
                doctorsCount
        );
    }
}
