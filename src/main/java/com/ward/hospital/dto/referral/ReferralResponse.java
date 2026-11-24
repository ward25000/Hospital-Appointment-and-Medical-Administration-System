package com.ward.hospital.dto.referral;

import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.PaymentStatus;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReferralResponse(
        Long id,
        Long patientId,
        String patientName,
        String patientIin,
        LocalDate patientBirthDate,
        String patientPhone,
        Long fromDoctorId,
        String fromDoctorName,
        String fromSpecialization,
        String fromOffice,
        Long toDoctorId,
        String toDoctorName,
        Long toSpecializationId,
        String toSpecializationName,
        Long appointmentId,
        LocalDateTime visitStartTime,
        String visitDiagnosis,
        String visitConclusion,
        Long serviceId,
        String serviceName,
        boolean diagnostic,
        BigDecimal price,
        PaymentStatus paymentStatus,
        String paymentStatusLabel,
        String department,
        String office,
        String reason,
        String status,
        String statusLabel,
        LocalDateTime decidedAt,
        LocalDateTime createdAt
) {

    public static ReferralResponse from(Referral referral) {
        String lang = I18n.lang();
        Doctor toDoctor = referral.getToDoctor();
        Specialization target = referral.getToSpecialization() != null ? referral.getToSpecialization()
                : toDoctor == null ? null : toDoctor.getSpecialization();
        return new ReferralResponse(
                referral.getId(),
                referral.getPatient().getId(),
                referral.getPatient().getFullName(),
                referral.getPatient().getIin(),
                referral.getPatient().getBirthDate(),
                referral.getPatient().getPhone(),
                referral.getFromDoctor().getId(),
                referral.getFromDoctor().getFullName(),
                referral.getFromDoctor().getSpecialization().name(lang),
                referral.getFromDoctor().getOffice(),
                toDoctor == null ? null : toDoctor.getId(),
                toDoctor == null ? null : toDoctor.getFullName(),
                referral.getToSpecialization() == null ? null : referral.getToSpecialization().getId(),
                target == null ? null : target.name(lang),
                referral.getAppointment() == null ? null : referral.getAppointment().getId(),
                referral.getAppointment() == null ? null : referral.getAppointment().getStartTime(),
                referral.getAppointment() == null ? null : referral.getAppointment().getDiagnosis(),
                referral.getAppointment() == null ? null : referral.getAppointment().getConclusion(),
                referral.getService() == null ? null : referral.getService().getId(),
                referral.getService() == null ? null : referral.getService().name(lang),
                referral.getService() != null && referral.getService().isDiagnostic(),
                referral.getPrice(),
                referral.getPaymentStatus(),
                referral.getPaymentStatus().getLabel(),
                department(referral, target, lang),
                referral.getOffice(),
                referral.getReason(),
                referral.getStatus().name(),
                referral.getStatus().getLabel(),
                referral.getDecidedAt(),
                referral.getCreatedAt()
        );
    }

    /** Department names copied from the directory follow the interface language; typed ones stay as is. */
    private static String department(Referral referral, Specialization target, String lang) {
        return target != null && target.getName().equals(referral.getDepartment())
                ? target.name(lang) : referral.getDepartment();
    }
}
