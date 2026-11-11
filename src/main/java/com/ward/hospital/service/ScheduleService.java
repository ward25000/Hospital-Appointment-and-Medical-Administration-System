package com.ward.hospital.service;

import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.dto.schedule.ScheduleRequest;
import com.ward.hospital.dto.schedule.ScheduleResponse;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public ScheduleService(DoctorScheduleRepository scheduleRepository, DoctorRepository doctorRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<ScheduleResponse> findByDoctor(Long doctorId) {
        getDoctor(doctorId);
        return scheduleRepository.findByDoctorId(doctorId).stream()
                .sorted(Comparator.comparing((DoctorSchedule schedule) -> schedule.getDayOfWeek().getValue())
                        .thenComparing(DoctorSchedule::getStartTime))
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(Long doctorId, ScheduleRequest request) {
        Doctor doctor = getDoctor(doctorId);
        validateTimes(request);
        if (scheduleRepository.existsByDoctorIdAndDayOfWeek(doctorId, request.dayOfWeek())) {
            throw new ConflictException("На этот день у врача уже настроено расписание");
        }
        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.slotMinutes() == null ? 30 : request.slotMinutes()
        );
        schedule.setActive(request.active() == null || request.active());
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request) {
        DoctorSchedule schedule = getEntity(id);
        validateTimes(request);
        if (scheduleRepository.existsByDoctorIdAndDayOfWeekAndIdNot(
                schedule.getDoctor().getId(), request.dayOfWeek(), id)) {
            throw new ConflictException("На этот день у врача уже настроено расписание");
        }
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        if (request.slotMinutes() != null) {
            schedule.setSlotMinutes(request.slotMinutes());
        }
        if (request.active() != null) {
            schedule.setActive(request.active());
        }
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepository.delete(getEntity(id));
    }

    private void validateTimes(ScheduleRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessRuleException("Время окончания смены должно быть позже начала");
        }
        int slotMinutes = request.slotMinutes() == null ? 30 : request.slotMinutes();
        long shiftMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (shiftMinutes < slotMinutes) {
            throw new BusinessRuleException("Смена короче одного интервала приёма");
        }
    }

    private DoctorSchedule getEntity(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Расписание не найдено"));
    }

    private Doctor getDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
    }
}
