package com.ward.hospital.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnlinePaymentRequest(
        @NotEmpty(message = "Выберите услугу для оплаты") List<@Valid PayableRef> items,
        @NotNull(message = "Укажите данные карты") @Valid CardRequest card
) {
}
