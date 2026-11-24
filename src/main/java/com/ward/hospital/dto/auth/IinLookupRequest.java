package com.ward.hospital.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IinLookupRequest(
        @NotBlank(message = "Укажите ИИН")
        @Pattern(regexp = "\\d{12}", message = "ИИН должен содержать 12 цифр")
        String iin
) {
}
