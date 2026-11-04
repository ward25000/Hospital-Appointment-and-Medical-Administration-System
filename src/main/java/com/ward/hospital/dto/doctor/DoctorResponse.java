package com.ward.hospital.dto.doctor;

import com.ward.hospital.domain.Doctor;

public record DoctorResponse(
        Long id,
        Long userId,
        String username,
        String firstName,
        String lastName,
        String fullName,
        Long specializationId,
        String specializationName,
        String email,
        String phone,
        String office,
        String bio,
        boolean active
) {

    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUser().getId(),
                doctor.getUser().getUsername(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getFullName(),
                doctor.getSpecialization().getId(),
                doctor.getSpecialization().getName(),
                doctor.getEmail(),
                doctor.getPhone(),
                doctor.getOffice(),
                doctor.getBio(),
                doctor.isActive()
        );
    }
}
