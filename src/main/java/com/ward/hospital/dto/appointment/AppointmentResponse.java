package com.ward.hospital.dto.appointment;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.PaymentStatus;
import com.ward.hospital.i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String doctorSpecialization,
        String doctorOffice,
        Long patientId,
        String patientName,
        String patientIin,
        String patientPhone,
        Long serviceId,
        String serviceName,
        int serviceDurationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String statusLabel,
        BigDecimal price,
        PaymentStatus paymentStatus,
        String paymentStatusLabel,
        LocalDateTime paidAt,
        String notes,
        String diagnosis,
        String conclusion,
        String recommendations,
        String cancellationReason,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt
) {

    public static AppointmentResponse from(Appointment appointment) {
        String lang = I18n.lang();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getDoctor().getSpecialization().name(lang),
                appointment.getDoctor().getOffice(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getPatient().getIin(),
                appointment.getPatient().getPhone(),
                appointment.getService().getId(),
                appointment.getService().name(lang),
                appointment.getService().getDurationMinutes(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getStatus().getLabel(),
                appointment.getPrice(),
                appointment.getPaymentStatus(),
                appointment.getPaymentStatus().getLabel(),
                appointment.getPaidAt(),
                appointment.getNotes(),
                appointment.getDiagnosis(),
                appointment.getConclusion(),
                appointment.getRecommendations(),
                appointment.getCancellationReason(),
                appointment.getCompletedAt(),
                appointment.getCancelledAt(),
                appointment.getCreatedAt()
        );
    }
}
