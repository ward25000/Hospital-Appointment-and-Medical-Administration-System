package com.ward.hospital.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull(message = "Выберите новую дату и время")
        @Future(message = "Новое время приёма должно быть в будущем")
        LocalDateTime startTime,

        @Size(max = 500, message = "Комментарий не должен превышать 500 символов")
        String notes
) {
}
