package com.ward.hospital.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CardRequest(
        @NotBlank(message = "Укажите номер карты")
        @Pattern(regexp = "[0-9 ]{12,23}", message = "Проверьте номер карты")
        String number,

        @NotBlank(message = "Укажите срок действия карты")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Срок действия в формате ММ/ГГ")
        String expiry,

        @NotBlank(message = "Укажите CVC")
        @Pattern(regexp = "\\d{3}", message = "CVC: 3 цифры")
        String cvc,

        @Size(max = 60, message = "Имя владельца слишком длинное")
        String holder
) {

    /** Card data must never end up in logs. */
    @Override
    public String toString() {
        return "CardRequest[hidden]";
    }
}
