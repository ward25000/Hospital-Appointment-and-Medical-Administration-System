package com.ward.hospital.dto.service;

import com.ward.hospital.domain.MedicalService;

import java.math.BigDecimal;

public record MedicalServiceResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int durationMinutes,
        boolean active
) {

    public static MedicalServiceResponse from(MedicalService service) {
        return new MedicalServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPrice(),
                service.getDurationMinutes(),
                service.isActive()
        );
    }
}
