package com.ward.hospital.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Введите логин")
        @Size(max = 60, message = "Логин слишком длинный")
        String username,

        @NotBlank(message = "Введите пароль")
        @Size(max = 100, message = "Пароль слишком длинный")
        String password
) {
}
