package com.ward.hospital.service;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.User;
import com.ward.hospital.dto.appointment.AppointmentCreateRequest;
import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.appointment.CancelRequest;
import com.ward.hospital.dto.appointment.RescheduleRequest;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.ForbiddenOperationException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private static final int MAX_RANGE_DAYS = 366;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              DoctorScheduleRepository scheduleRepository,
                              PatientRepository patientRepository,
                              MedicalServiceRepository medicalServiceRepository,
                              UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.patientRepository = patientRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.userRepository = userRepository;
    }

    public List<AppointmentResponse> findAll(LocalDate from, LocalDate to, Long doctorId,
                                             Long patientId, AppointmentStatus status,
                                             String query, UserPrincipal actor) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now().plusMonths(2);
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessRuleException("Дата окончания периода не может быть раньше даты начала");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("Период не должен превышать " + MAX_RANGE_DAYS + " дней");
        }

        Long effectiveDoctorId = doctorId;
        if (actor.isDoctor()) {
            effectiveDoctorId = actor.getDoctorId();
        }

        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        return appointmentRepository.search(
                        effectiveFrom.atStartOfDay(),
                        effectiveTo.plusDays(1).atStartOfDay(),
                        effectiveDoctorId,
                        patientId,
                        status,
                        normalizedQuery)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    public AppointmentResponse findById(Long id, UserPrincipal actor) {
        Appointment appointment = getDetailed(id);
        checkViewAccess(appointment, actor);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse create(AppointmentCreateRequest request, UserPrincipal actor) {
        requireScheduler(actor);
        LocalDateTime start = normalizeStart(request.startTime());

        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("Врач сейчас не ведёт приём");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
        MedicalService service = medicalServiceRepository.findById(request.serviceId())
                .orElseThrow(() -> new NotFoundException("Услуга не найдена"));
        if (!service.isActive()) {
            throw new BusinessRuleException("Выбранная услуга недоступна");
        }

        LocalDateTime end = validateBooking(doctor, service, start, null);
        User creator = userRepository.findById(actor.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Appointment appointment = new Appointment(doctor, patient, service, start, end,
                service.getPrice(), creator);
        appointment.setNotes(normalize(request.notes()));
        try {
            appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Это время уже занято. Выберите другой слот");
        }
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, RescheduleRequest request, UserPrincipal actor) {
        requireScheduler(actor);
        Appointment appointment = getDetailed(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Перенести можно только запланированный приём");
        }

        LocalDateTime start = normalizeStart(request.startTime());
        Doctor doctor = doctorRepository.findByIdForUpdate(appointment.getDoctor().getId())
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
        if (!doctor.isActive()) {
            throw new BusinessRuleException("Врач сейчас не ведёт приём");
        }

        LocalDateTime end = validateBooking(doctor, appointment.getService(), start, appointment.getId());
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        if (request.notes() != null) {
            appointment.setNotes(normalize(request.notes()));
        }
        try {
            appointmentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Это время уже занято. Выберите другой слот");
        }
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse complete(Long id, UserPrincipal actor) {
        Appointment appointment = getDetailed(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Завершить можно только запланированный приём");
        }
        if (!actor.isAdmin()) {
            if (!actor.isDoctor() || !Objects.equals(actor.getDoctorId(), appointment.getDoctor().getId())) {
                throw new ForbiddenOperationException("Завершить приём может только назначенный врач");
            }
        }
        if (appointment.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Нельзя завершить приём до его начала");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse cancel(Long id, CancelRequest request, UserPrincipal actor) {
        Appointment appointment = getDetailed(id);
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Отменить можно только запланированный приём");
        }
        if (actor.isDoctor() && !Objects.equals(actor.getDoctorId(), appointment.getDoctor().getId())) {
            throw new ForbiddenOperationException("Врач может отменить только свой приём");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancellationReason(normalize(request.reason()));
        return AppointmentResponse.from(appointment);
    }

    private LocalDateTime validateBooking(Doctor doctor, MedicalService service,
                                          LocalDateTime start, Long excludeAppointmentId) {
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Время приёма должно быть в будущем");
        }
        LocalDateTime end = start.plusMinutes(service.getDurationMinutes());
        DoctorSchedule schedule = scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), start.getDayOfWeek())
                .orElseThrow(() -> new BusinessRuleException(
                        "В выбранный день врач не принимает. Выберите другую дату"));

        if (start.toLocalTime().isBefore(schedule.getStartTime())
                || end.toLocalTime().isAfter(schedule.getEndTime())
                || !end.toLocalDate().equals(start.toLocalDate())) {
            throw new BusinessRuleException("Время приёма выходит за рамки рабочей смены врача");
        }

        long offsetMinutes = Duration.between(schedule.getStartTime(), start.toLocalTime()).toMinutes();
        if (offsetMinutes % schedule.getSlotMinutes() != 0) {
            throw new BusinessRuleException("Время приёма должно быть кратно интервалу "
                    + schedule.getSlotMinutes() + " минут");
        }

        long conflicts = appointmentRepository.countOverlapping(
                doctor.getId(), start, end, AppointmentStatus.CANCELLED, excludeAppointmentId);
        if (conflicts > 0) {
            throw new ConflictException("Это время уже занято. Выберите другой слот");
        }
        return end;
    }

    private void checkViewAccess(Appointment appointment, UserPrincipal actor) {
        if (actor.isDoctor() && !Objects.equals(actor.getDoctorId(), appointment.getDoctor().getId())) {
            throw new ForbiddenOperationException("Врач может просматривать только свои приёмы");
        }
    }

    private void requireScheduler(UserPrincipal actor) {
        if (!(actor.isAdmin() || actor.isReceptionist())) {
            throw new ForbiddenOperationException(
                    "Создавать и переносить записи могут администратор или регистратор");
        }
    }

    private Appointment getDetailed(Long id) {
        return appointmentRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Приём не найден"));
    }

    private LocalDateTime normalizeStart(LocalDateTime start) {
        if (start.getSecond() != 0 || start.getNano() != 0) {
            throw new BusinessRuleException("Время приёма указывается с точностью до минуты");
        }
        return start.withSecond(0).withNano(0);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
