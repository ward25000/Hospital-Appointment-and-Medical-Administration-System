package com.ward.hospital.dto.doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorUpdateRequest(
        @NotBlank(message = "Укажите имя врача")
        @Size(max = 80, message = "Имя не должно превышать 80 символов")
        String firstName,

        @NotBlank(message = "Укажите фамилию врача")
        @Size(max = 80, message = "Фамилия не должна превышать 80 символов")
        String lastName,

        @NotNull(message = "Выберите специализацию")
        Long specializationId,

        @NotBlank(message = "Укажите email")
        @Email(message = "Некорректный email")
        @Size(max = 160, message = "Email слишком длинный")
        String email,

        @NotBlank(message = "Укажите телефон")
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,25}$", message = "Некорректный телефон")
        String phone,

        @NotBlank(message = "Укажите кабинет")
        @Size(max = 30, message = "Номер кабинета слишком длинный")
        String office,

        @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
        String bio,

        Boolean active
) {
}
