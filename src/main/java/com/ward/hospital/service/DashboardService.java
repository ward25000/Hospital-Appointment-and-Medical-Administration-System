package com.ward.hospital.service;

import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.dashboard.DashboardResponse;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AppointmentRepository appointmentRepository;

    public DashboardService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public DashboardResponse getDashboard(UserPrincipal actor) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atTime(LocalTime.MIN);
        LocalDateTime nextMonthStart = today.withDayOfMonth(1).plusMonths(1).atTime(LocalTime.MIN);

        Long doctorId = actor.isDoctor() ? actor.getDoctorId() : null;

        long todayTotal = doctorId == null
                ? appointmentRepository.countByStartTimeBetween(todayStart, tomorrowStart)
                : appointmentRepository.countByDoctorIdAndStartTimeBetween(doctorId, todayStart, tomorrowStart);

        long upcomingTotal = doctorId == null
                ? appointmentRepository.countByStatusAndStartTimeAfter(AppointmentStatus.SCHEDULED, now)
                : appointmentRepository.countByStatusAndDoctorIdAndStartTimeAfter(
                        AppointmentStatus.SCHEDULED, doctorId, now);

        long completedThisMonth = doctorId == null
                ? appointmentRepository.countByStatusAndStartTimeBetween(
                        AppointmentStatus.COMPLETED, monthStart, nextMonthStart)
                : appointmentRepository.countByStatusAndDoctorIdAndStartTimeBetween(
                        AppointmentStatus.COMPLETED, doctorId, monthStart, nextMonthStart);

        long patientsThisMonth = appointmentRepository.countDistinctPatientsInPeriod(
                monthStart, nextMonthStart, doctorId);
        BigDecimal revenueThisMonth = appointmentRepository.sumRevenue(
                AppointmentStatus.COMPLETED, monthStart, nextMonthStart, doctorId);

        List<AppointmentResponse> todayAppointments = appointmentRepository
                .search(todayStart, tomorrowStart, doctorId, null, null, null)
                .stream()
                .limit(8)
                .map(AppointmentResponse::from)
                .toList();

        List<AppointmentResponse> upcoming = (doctorId == null
                ? appointmentRepository.findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(
                        AppointmentStatus.SCHEDULED, now)
                : appointmentRepository.findTop5ByDoctorIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                        doctorId, AppointmentStatus.SCHEDULED, now))
                .stream()
                .map(AppointmentResponse::from)
                .toList();

        return new DashboardResponse(
                today,
                todayTotal,
                upcomingTotal,
                completedThisMonth,
                patientsThisMonth,
                revenueThisMonth,
                todayAppointments,
                upcoming
        );
    }
}
