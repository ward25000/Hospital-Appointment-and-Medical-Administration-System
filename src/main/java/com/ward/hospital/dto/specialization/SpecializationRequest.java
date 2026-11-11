package com.ward.hospital.dto.specialization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecializationRequest(
        @NotBlank(message = "Укажите название специализации")
        @Size(max = 120, message = "Название не должно превышать 120 символов")
        String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description,

        Boolean active
) {
}
