package com.ward.hospital.dto.payment;

import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.PaymentMethod;
import com.ward.hospital.domain.ReceiptEmailStatus;
import com.ward.hospital.i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        String number,
        Long patientId,
        String patientName,
        String patientIin,
        BigDecimal amount,
        PaymentMethod method,
        String methodLabel,
        String cardLast4,
        LocalDateTime paidAt,
        String email,
        ReceiptEmailStatus emailStatus,
        String emailStatusLabel,
        List<Item> items
) {

    public record Item(PayableType type, Long id, String serviceName, BigDecimal amount) {
    }

    public static PaymentResponse from(Payment payment) {
        String lang = I18n.lang();
        return new PaymentResponse(
                payment.getId(),
                payment.getNumber(),
                payment.getPatient().getId(),
                payment.getPatient().getFullName(),
                payment.getPatient().getIin(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getMethod().getLabel(),
                payment.getCardLast4(),
                payment.getPaidAt(),
                payment.getEmail(),
                payment.getEmailStatus(),
                payment.getEmailStatus().getLabel(),
                payment.getItems().stream()
                        .map(item -> item.getAppointment() != null
                                ? new Item(PayableType.APPOINTMENT, item.getAppointment().getId(),
                                item.getService().name(lang), item.getAmount())
                                : new Item(PayableType.REFERRAL, item.getReferral().getId(),
                                item.getService().name(lang), item.getAmount()))
                        .toList()
        );
    }
}
