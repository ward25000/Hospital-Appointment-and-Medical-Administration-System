package com.ward.hospital.dto.patient;

import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.Patient;

import java.time.LocalDate;
import java.time.Period;

public record PatientResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        LocalDate birthDate,
        int age,
        Gender gender,
        String genderLabel,
        String phone,
        String email,
        String address
) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getFullName(),
                patient.getBirthDate(),
                Period.between(patient.getBirthDate(), LocalDate.now()).getYears(),
                patient.getGender(),
                patient.getGender().getLabel(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress()
        );
    }
}
