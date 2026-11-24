package com.ward.hospital.dto.payment;

import jakarta.validation.constraints.NotNull;

public record PayableRef(
        @NotNull(message = "Выберите услугу для оплаты") PayableType type,
        @NotNull(message = "Выберите услугу для оплаты") Long id
) {
}
