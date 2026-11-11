package com.ward.hospital.service;

import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.dto.appointment.AvailabilityResponse;
import com.ward.hospital.dto.appointment.SlotResponse;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.util.DayOfWeekLabels;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final AppointmentRepository appointmentRepository;

    public AvailabilityService(DoctorRepository doctorRepository,
                               DoctorScheduleRepository scheduleRepository,
                               MedicalServiceRepository medicalServiceRepository,
                               AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public AvailabilityResponse getAvailability(Long doctorId, LocalDate date, Long serviceId,
                                                Long excludeAppointmentId) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Нельзя выбрать прошедшую дату");
        }
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("Врач сейчас не ведёт приём");
        }

        MedicalService service = null;
        if (serviceId != null) {
            service = medicalServiceRepository.findById(serviceId)
                    .orElseThrow(() -> new NotFoundException("Услуга не найдена"));
        }

        DoctorSchedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, date.getDayOfWeek())
                .orElse(null);

        if (schedule == null) {
            return new AvailabilityResponse(
                    doctor.getId(),
                    doctor.getFullName(),
                    date,
                    date.getDayOfWeek(),
                    DayOfWeekLabels.of(date.getDayOfWeek()),
                    false,
                    "В этот день врач не принимает",
                    0,
                    service != null ? service.getDurationMinutes() : 0,
                    List.of()
            );
        }

        int durationMinutes = service != null ? service.getDurationMinutes() : schedule.getSlotMinutes();
        long duration = durationMinutes;
        List<SlotResponse> slots = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (LocalTime slot = schedule.getStartTime();
             slot.isBefore(schedule.getEndTime());
             slot = slot.plusMinutes(schedule.getSlotMinutes())) {
            LocalDateTime start = LocalDateTime.of(date, slot);
            LocalDateTime end = start.plusMinutes(duration);

            boolean available = true;
            String reason = null;
            if (end.toLocalTime().isAfter(schedule.getEndTime())
                    || !end.toLocalDate().equals(date)) {
                available = false;
                reason = "Недостаточно времени до конца смены";
            } else if (!start.isAfter(now)) {
                available = false;
                reason = "Время уже прошло";
            } else {
                long conflicts = appointmentRepository.countOverlapping(
                        doctorId, start, end, AppointmentStatus.CANCELLED, excludeAppointmentId);
                if (conflicts > 0) {
                    available = false;
                    reason = "Занято";
                }
            }
            slots.add(new SlotResponse(start, end, available, reason));
        }

        long availableCount = slots.stream().filter(SlotResponse::available).count();
        String message = availableCount == 0
                ? "На эту дату нет свободных слотов"
                : "Свободных слотов: " + availableCount;

        return new AvailabilityResponse(
                doctor.getId(),
                doctor.getFullName(),
                date,
                date.getDayOfWeek(),
                DayOfWeekLabels.of(date.getDayOfWeek()),
                true,
                message,
                schedule.getSlotMinutes(),
                durationMinutes,
                slots
        );
    }
}
