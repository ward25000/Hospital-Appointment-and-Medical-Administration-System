package com.ward.hospital.dto.appointment;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;

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
        String patientPhone,
        Long serviceId,
        String serviceName,
        int serviceDurationMinutes,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentStatus status,
        String statusLabel,
        BigDecimal price,
        String notes,
        String cancellationReason,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt
) {

    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(),
                appointment.getDoctor().getSpecialization().getName(),
                appointment.getDoctor().getOffice(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFullName(),
                appointment.getPatient().getPhone(),
                appointment.getService().getId(),
                appointment.getService().getName(),
                appointment.getService().getDurationMinutes(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getStatus().getLabel(),
                appointment.getPrice(),
                appointment.getNotes(),
                appointment.getCancellationReason(),
                appointment.getCompletedAt(),
                appointment.getCancelledAt(),
                appointment.getCreatedAt()
        );
    }
}
