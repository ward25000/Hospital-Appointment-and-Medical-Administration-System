package com.ward.hospital.dto.referral;

import com.ward.hospital.domain.Referral;

import java.time.LocalDateTime;

public record ReferralResponse(
        Long id,
        Long patientId,
        String patientName,
        Long fromDoctorId,
        String fromDoctorName,
        String fromSpecialization,
        Long toDoctorId,
        String toDoctorName,
        Long toSpecializationId,
        String toSpecializationName,
        Long appointmentId,
        LocalDateTime visitStartTime,
        String department,
        String office,
        String reason,
        String status,
        String statusLabel,
        LocalDateTime decidedAt,
        LocalDateTime createdAt
) {

    public static ReferralResponse from(Referral referral) {
        return new ReferralResponse(
                referral.getId(),
                referral.getPatient().getId(),
                referral.getPatient().getFullName(),
                referral.getFromDoctor().getId(),
                referral.getFromDoctor().getFullName(),
                referral.getFromDoctor().getSpecialization().getName(),
                referral.getToDoctor() == null ? null : referral.getToDoctor().getId(),
                referral.getToDoctor() == null ? null : referral.getToDoctor().getFullName(),
                referral.getToSpecialization() == null ? null : referral.getToSpecialization().getId(),
                referral.getToSpecialization() == null ? null : referral.getToSpecialization().getName(),
                referral.getAppointment() == null ? null : referral.getAppointment().getId(),
                referral.getAppointment() == null ? null : referral.getAppointment().getStartTime(),
                referral.getDepartment(),
                referral.getOffice(),
                referral.getReason(),
                referral.getStatus().name(),
                referral.getStatus().getLabel(),
                referral.getDecidedAt(),
                referral.getCreatedAt()
        );
    }
}
