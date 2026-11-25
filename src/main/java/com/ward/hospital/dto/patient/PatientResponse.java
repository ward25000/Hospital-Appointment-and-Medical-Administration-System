package com.ward.hospital.dto.patient;

import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.i18n.I18n;

import java.time.LocalDate;
import java.time.Period;

public record PatientResponse(
        Long id,
        String iin,
        Long userId,
        String firstName,
        String lastName,
        String middleName,
        String fullName,
        LocalDate birthDate,
        int age,
        Gender gender,
        String genderLabel,
        String phone,
        String email,
        String address,
        String city,
        String street,
        String house,
        String apartment
) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getIin(),
                patient.getUser() == null ? null : patient.getUser().getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getMiddleName(),
                patient.getFullName(),
                patient.getBirthDate(),
                Period.between(patient.getBirthDate(), LocalDate.now()).getYears(),
                patient.getGender(),
                patient.getGender().getLabel(),
                patient.getPhone(),
                patient.getEmail(),
                patient.addressText(I18n.lang()),
                patient.getAddressCity(),
                patient.getAddressStreet(),
                patient.getAddressHouse(),
                patient.getAddressApartment()
        );
    }
}
