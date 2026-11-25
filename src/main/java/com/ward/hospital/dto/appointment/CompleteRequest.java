package com.ward.hospital.dto.appointment;

import jakarta.validation.constraints.Size;

public record CompleteRequest(
        @Size(max = 500, message = "Диагноз не должен превышать 500 символов")
        String diagnosis,

        @Size(max = 2000, message = "Заключение не должно превышать 2000 символов")
        String conclusion,

        @Size(max = 2000, message = "Рекомендации не должны превышать 2000 символов")
        String recommendations
) {
}
