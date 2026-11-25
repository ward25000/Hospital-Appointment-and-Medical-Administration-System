package com.ward.hospital.dto.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LanguageRequest(
        @NotBlank(message = "Укажите язык")
        @Pattern(regexp = "kk|ru|en", message = "Укажите язык")
        String lang
) {
}
