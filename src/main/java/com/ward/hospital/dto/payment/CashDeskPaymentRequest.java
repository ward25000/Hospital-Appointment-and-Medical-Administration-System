package com.ward.hospital.dto.payment;

import com.ward.hospital.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CashDeskPaymentRequest(
        @NotNull(message = "Выберите пациента") Long patientId,
        @NotEmpty(message = "Выберите услугу для оплаты") List<@Valid PayableRef> items,
        @NotNull(message = "Выберите способ оплаты") PaymentMethod method
) {
}
