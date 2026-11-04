package com.ward.hospital.dto.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MedicalServiceRequest(
        @NotBlank(message = "Укажите название услуги")
        @Size(max = 150, message = "Название не должно превышать 150 символов")
        String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description,

        @NotNull(message = "Укажите стоимость услуги")
        @DecimalMin(value = "0.00", message = "Стоимость не может быть отрицательной")
        @Digits(integer = 8, fraction = 2, message = "Стоимость должна содержать не более 2 знаков после запятой")
        BigDecimal price,

        @NotNull(message = "Укажите длительность услуги")
        @Min(value = 5, message = "Длительность должна быть не менее 5 минут")
        @Max(value = 240, message = "Длительность должна быть не более 240 минут")
        Integer durationMinutes,

        Boolean active
) {
}
