package com.ward.hospital.dto.dashboard;

import com.ward.hospital.dto.appointment.AppointmentResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate date,
        long todayTotal,
        long upcomingTotal,
        long completedThisMonth,
        long uniquePatientsThisMonth,
        BigDecimal revenueThisMonth,
        List<AppointmentResponse> todayAppointments,
        List<AppointmentResponse> upcomingAppointments
) {
}
