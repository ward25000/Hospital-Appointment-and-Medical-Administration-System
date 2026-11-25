package com.ward.hospital.dto.doctor;

import com.ward.hospital.i18n.I18n;
import com.ward.hospital.domain.Doctor;

public record DoctorResponse(
        Long id,
        Long userId,
        String username,
        String firstName,
        String lastName,
        String middleName,
        String fullName,
        Long specializationId,
        String specializationName,
        String email,
        String phone,
        String office,
        String bio,
        String bioRu,
        String bioKk,
        String bioEn,
        boolean active
) {

    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUser().getId(),
                doctor.getUser().getUsername(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getMiddleName(),
                doctor.getFullName(),
                doctor.getSpecialization().getId(),
                doctor.getSpecialization().name(I18n.lang()),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getOffice(),
                doctor.bio(I18n.lang()),
                doctor.getBio(),
                doctor.getBioKk(),
                doctor.getBioEn(),
                doctor.isActive()
        );
    }
}
