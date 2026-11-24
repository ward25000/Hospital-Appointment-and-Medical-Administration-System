package com.ward.hospital.payment;

import com.ward.hospital.dto.payment.CardRequest;

import java.math.BigDecimal;

/** Acquirer integration point: replace the demo implementation with a real bank gateway. */
public interface PaymentGateway {

    Charge charge(String paymentNumber, BigDecimal amount, CardRequest card);

    record Charge(String reference, String cardLast4) {
    }
}
