package com.ward.hospital.dto.auth;

import com.ward.hospital.domain.Gender;
import com.ward.hospital.util.Address;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistrationRequest(
        @Pattern(regexp = "[A-Za-z0-9._-]{3,60}", message = "Логин: 3–60 латинских букв, цифр, точки, дефиса или подчёркивания")
        String username,

        @NotBlank(message = "Укажите пароль")
        @Size(min = 8, max = 100, message = "Пароль должен содержать от 8 до 100 символов")
        String password,

        @NotBlank(message = "Укажите ИИН")
        @Pattern(regexp = "\\d{12}", message = "ИИН должен содержать 12 цифр")
        String iin,

        @NotBlank(message = "Укажите имя")
        @Size(max = 80, message = "Имя не должно превышать 80 символов")
        String firstName,

        @NotBlank(message = "Укажите фамилию")
        @Size(max = 80, message = "Фамилия не должна превышать 80 символов")
        String lastName,

        @Size(max = 80, message = "Отчество не должно превышать 80 символов")
        String middleName,

        @NotNull(message = "Укажите дату рождения")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @NotNull(message = "Укажите пол")
        Gender gender,

        @NotBlank(message = "Укажите телефон")
        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,25}$", message = "Некорректный телефон")
        String phone,

        @NotBlank(message = "Укажите email")
        @Email(message = "Некорректный email")
        @Size(max = 160, message = "Email слишком длинный")
        String email,

        @Size(max = 300, message = "Адрес не должен превышать 300 символов")
        String address,

        @Size(max = 100, message = "Адрес не должен превышать 300 символов")
        String city,

        @Size(max = 120, message = "Адрес не должен превышать 300 символов")
        String street,

        @Size(max = 20, message = "Адрес не должен превышать 300 символов")
        String house,

        @Size(max = 20, message = "Адрес не должен превышать 300 символов")
        String apartment
) {

    @AssertTrue(message = "Укажите адрес проживания: город, улицу и дом")
    public boolean isAddressProvided() {
        return structuredAddress().isComplete() || (address != null && !address.isBlank());
    }

    public Address structuredAddress() {
        return Address.of(city, street, house, apartment);
    }
}
