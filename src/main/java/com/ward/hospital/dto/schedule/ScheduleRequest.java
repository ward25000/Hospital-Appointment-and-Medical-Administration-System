package com.ward.hospital.dto.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull(message = "Выберите день недели")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Укажите начало смены")
        LocalTime startTime,

        @NotNull(message = "Укажите окончание смены")
        LocalTime endTime,

        @Min(value = 10, message = "Интервал должен быть не менее 10 минут")
        @Max(value = 120, message = "Интервал должен быть не более 120 минут")
        Integer slotMinutes,

        Boolean active
) {
}
