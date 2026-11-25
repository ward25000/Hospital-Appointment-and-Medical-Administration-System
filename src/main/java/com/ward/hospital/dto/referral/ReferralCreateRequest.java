package com.ward.hospital.dto.referral;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The recipient is a doctor, a department, or a diagnostic room. For a diagnostic
 * room only {@code serviceId} is sent: the department is taken from the service.
 */
public record ReferralCreateRequest(
        @NotNull(message = "Укажите приём, во время которого выписано направление")
        Long appointmentId,

        Long toDoctorId,

        Long toSpecializationId,

        Long serviceId,

        @Size(max = 120, message = "Название отделения не должно превышать 120 символов")
        String department,

        @Size(max = 30, message = "Номер кабинета не должен превышать 30 символов")
        String office,

        @NotBlank(message = "Укажите причину направления")
        @Size(max = 500, message = "Причина направления не должна превышать 500 символов")
        String reason
) {
}
