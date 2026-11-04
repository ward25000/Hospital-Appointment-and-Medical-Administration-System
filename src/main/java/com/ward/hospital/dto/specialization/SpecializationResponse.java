package com.ward.hospital.dto.specialization;

import com.ward.hospital.domain.Specialization;

public record SpecializationResponse(
        Long id,
        String name,
        String description,
        boolean active,
        long doctorsCount
) {

    public static SpecializationResponse from(Specialization specialization, long doctorsCount) {
        return new SpecializationResponse(
                specialization.getId(),
                specialization.getName(),
                specialization.getDescription(),
                specialization.isActive(),
                doctorsCount
        );
    }
}
