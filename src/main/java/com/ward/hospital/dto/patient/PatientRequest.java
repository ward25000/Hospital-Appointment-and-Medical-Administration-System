package com.ward.hospital.dto.patient;

import com.ward.hospital.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank(message = "Укажите имя пациента")
        @Size(max = 80, message = "Имя не должно превышать 80 символов")
        String firstName,

        @NotBlank(message = "Укажите фамилию пациента")
        @Size(max = 80, message = "Фамилия не должна превышать 80 символов")
        String lastName,

        @NotNull(message = "Укажите дату рождения")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @NotNull(message = "Укажите пол")
        Gender gender,

        @NotBlank(message = "Укажите телефон")
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,25}$", message = "Некорректный телефон")
        String phone,

        @Email(message = "Некорректный email")
        @Size(max = 160, message = "Email слишком длинный")
        String email,

        @Size(max = 300, message = "Адрес не должен превышать 300 символов")
        String address
) {
}
