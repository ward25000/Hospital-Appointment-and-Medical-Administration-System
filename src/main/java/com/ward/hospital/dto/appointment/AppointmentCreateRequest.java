package com.ward.hospital.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AppointmentCreateRequest(
        @NotNull(message = "Выберите врача")
        Long doctorId,

        @NotNull(message = "Выберите пациента")
        Long patientId,

        @NotNull(message = "Выберите услугу")
        Long serviceId,

        @NotNull(message = "Выберите дату и время приёма")
        @Future(message = "Время приёма должно быть в будущем")
        LocalDateTime startTime,

        @Size(max = 500, message = "Комментарий не должен превышать 500 символов")
        String notes
) {
}
