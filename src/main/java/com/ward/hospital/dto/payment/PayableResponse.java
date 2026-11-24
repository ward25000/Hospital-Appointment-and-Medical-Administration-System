package com.ward.hospital.dto.payment;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A visit or a referral that is waiting for payment. */
public record PayableResponse(
        PayableType type,
        Long id,
        String serviceName,
        String doctorName,
        LocalDateTime date,
        BigDecimal amount
) {

    public static PayableResponse from(Appointment appointment) {
        return new PayableResponse(PayableType.APPOINTMENT, appointment.getId(),
                appointment.getService().name(I18n.lang()), appointment.getDoctor().getFullName(),
                appointment.getStartTime(), appointment.getPrice());
    }

    public static PayableResponse from(Referral referral) {
        return new PayableResponse(PayableType.REFERRAL, referral.getId(),
                referral.getService().name(I18n.lang()), referral.getFromDoctor().getFullName(),
                referral.getCreatedAt(), referral.getPrice());
    }
}
