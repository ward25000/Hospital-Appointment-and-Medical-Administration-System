package com.ward.hospital.service;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.domain.User;
import com.ward.hospital.dto.appointment.AppointmentCreateRequest;
import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.appointment.AvailabilityResponse;
import com.ward.hospital.dto.appointment.CancelRequest;
import com.ward.hospital.dto.appointment.RescheduleRequest;
import com.ward.hospital.dto.appointment.SlotResponse;
import com.ward.hospital.dto.report.ReportResponse;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.ForbiddenOperationException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Appointment booking engine integration tests")
class AppointmentServiceIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository scheduleRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private SpecializationRepository specializationRepository;

    @Autowired
    private UserRepository userRepository;

    private LocalDate workingDate;
    private User admin;
    private User doctorUser;
    private User secondDoctorUser;
    private User receptionist;
    private Doctor doctor;
    private Doctor secondDoctor;
    private Patient patient;
    private MedicalService shortService;
    private MedicalService longService;
    private UserPrincipal adminPrincipal;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal receptionistPrincipal;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        workingDate = LocalDate.now().plusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        Specialization specialization = specializationRepository.save(
                new Specialization("Кардиология " + suffix, "Тестовое направление"));
        admin = userRepository.save(new User("admin-" + suffix, "$2a$10$test", "Администратор Тестов", Role.ADMIN));
        doctorUser = userRepository.save(new User("doctor-" + suffix, "$2a$10$test", "Иван Врач", Role.DOCTOR));
        secondDoctorUser = userRepository.save(new User("doctor2-" + suffix, "$2a$10$test", "Пётр Второй", Role.DOCTOR));
        receptionist = userRepository.save(new User("reception-" + suffix, "$2a$10$test", "Регистратор Тестов", Role.RECEPTIONIST));

        doctor = doctorRepository.save(new Doctor(doctorUser, specialization, "Иван", "Врач",
                "doctor-" + suffix + "@example.com", "+7 (900) 000-00-01", "101"));
        secondDoctor = doctorRepository.save(new Doctor(secondDoctorUser, specialization, "Пётр", "Второй",
                "doctor2-" + suffix + "@example.com", "+7 (900) 000-00-02", "102"));

        scheduleRepository.save(new DoctorSchedule(doctor, workingDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 30));
        scheduleRepository.save(new DoctorSchedule(secondDoctor, workingDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 30));

        patient = patientRepository.save(new Patient("Пациент", "Тестов", LocalDate.of(1990, 5, 15),
                Gender.MALE, "+7 (916) 555-44-33", "patient@example.com", "Москва"));
        shortService = medicalServiceRepository.save(new MedicalService(
                "Консультация " + suffix, "Первичный приём", new BigDecimal("2500.00"), 30));
        longService = medicalServiceRepository.save(new MedicalService(
                "Расширенный приём " + suffix, "Приём повышенной длительности", new BigDecimal("3500.00"), 45));

        adminPrincipal = UserPrincipal.from(admin, null);
        doctorPrincipal = UserPrincipal.from(doctorUser, doctor.getId());
        receptionistPrincipal = UserPrincipal.from(receptionist, null);
    }

    @Test
    @DisplayName("Books a free slot and calculates the end time and price")
    void booksAvailableSlot() {
        AppointmentResponse response = appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(10, 0), "Первичная запись"),
                adminPrincipal);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.startTime()).isEqualTo(workingDate.atTime(10, 0));
        assertThat(response.endTime()).isEqualTo(workingDate.atTime(10, 30));
        assertThat(response.price()).isEqualByComparingTo("2500.00");
        assertThat(response.doctorName()).isEqualTo("Иван Врач");
        assertThat(response.patientName()).isEqualTo("Пациент Тестов");
        assertThat(appointmentRepository.countByDoctorId(doctor.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects an overlapping interval even when the start time is different")
    void rejectsOverlappingAppointment() {
        appointmentService.create(new AppointmentCreateRequest(doctor.getId(), patient.getId(),
                longService.getId(), workingDate.atTime(10, 0), null), adminPrincipal);

        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(10, 30), null), adminPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже занято");

        assertThat(appointmentRepository.countByDoctorId(doctor.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects a time outside the doctor's shift")
    void rejectsTimeOutsideShift() {
        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(8, 30), null), adminPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("рабочей смены");
    }

    @Test
    @DisplayName("Rejects a start time that is not aligned to the schedule slot")
    void rejectsMisalignedSlot() {
        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(10, 15), null), adminPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("кратно");
    }

    @Test
    @DisplayName("Rejects a booking on a day without an active schedule")
    void rejectsDayWithoutSchedule() {
        LocalDate dayOff = workingDate.plusDays(1);
        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        dayOff.atTime(10, 0), null), adminPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("не принимает");
    }

    @Test
    @DisplayName("Rejects a booking in the past")
    void rejectsPastBooking() {
        LocalDateTime pastStart = LocalDateTime.now().minusHours(2)
                .withSecond(0)
                .withNano(0);
        assertThatThrownBy(() -> appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        pastStart, null), adminPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("будущем");
    }

    @Test
    @DisplayName("Availability marks booked slots as unavailable and keeps adjacent slots free")
    void availabilityReflectsBookedSlots() {
        appointmentService.create(new AppointmentCreateRequest(doctor.getId(), patient.getId(),
                shortService.getId(), workingDate.atTime(10, 0), null), adminPrincipal);

        AvailabilityResponse availability = availabilityService.getAvailability(
                doctor.getId(), workingDate, shortService.getId(), null);

        assertThat(availability.workingDay()).isTrue();
        assertThat(availability.slots().stream().filter(SlotResponse::available).count()).isEqualTo(15);
        assertThat(slotAt(availability, 10, 0).available()).isFalse();
        assertThat(slotAt(availability, 10, 0).reason()).isEqualTo("Занято");
        assertThat(slotAt(availability, 10, 30).available()).isTrue();
    }

    @Test
    @DisplayName("Cancellation frees the slot for a new appointment")
    void cancellationFreesSlot() {
        AppointmentResponse created = appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(11, 0), null), adminPrincipal);

        appointmentService.cancel(created.id(), new CancelRequest("Пациент перенёс визит"), adminPrincipal);

        AppointmentResponse afterCancel = appointmentService.findById(created.id(), adminPrincipal);
        assertThat(afterCancel.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(afterCancel.cancellationReason()).isEqualTo("Пациент перенёс визит");

        AvailabilityResponse availability = availabilityService.getAvailability(
                doctor.getId(), workingDate, shortService.getId(), null);
        assertThat(slotAt(availability, 11, 0).available()).isTrue();
        assertThat(availability.slots().stream().filter(SlotResponse::available).count()).isEqualTo(16);
    }

    @Test
    @DisplayName("Reschedule rejects an occupied interval and accepts a free one")
    void rescheduleValidatesConflicts() {
        AppointmentResponse first = appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(10, 0), null), adminPrincipal);
        appointmentService.create(new AppointmentCreateRequest(doctor.getId(), patient.getId(),
                shortService.getId(), workingDate.atTime(11, 0), null), adminPrincipal);

        assertThatThrownBy(() -> appointmentService.reschedule(first.id(),
                new RescheduleRequest(workingDate.atTime(11, 0), null), adminPrincipal))
                .isInstanceOf(ConflictException.class);

        AppointmentResponse moved = appointmentService.reschedule(first.id(),
                new RescheduleRequest(workingDate.atTime(12, 0), "Новое время"), adminPrincipal);
        assertThat(moved.startTime()).isEqualTo(workingDate.atTime(12, 0));
        assertThat(moved.endTime()).isEqualTo(workingDate.atTime(12, 30));
        assertThat(moved.notes()).isEqualTo("Новое время");
    }

    @Test
    @DisplayName("Availability can exclude the appointment being rescheduled")
    void availabilityExcludesCurrentAppointment() {
        AppointmentResponse created = appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(10, 0), null), adminPrincipal);

        AvailabilityResponse withoutExclusion = availabilityService.getAvailability(
                doctor.getId(), workingDate, shortService.getId(), null);
        AvailabilityResponse withExclusion = availabilityService.getAvailability(
                doctor.getId(), workingDate, shortService.getId(), created.id());

        assertThat(slotAt(withoutExclusion, 10, 0).available()).isFalse();
        assertThat(slotAt(withExclusion, 10, 0).available()).isTrue();
    }

    @Test
    @DisplayName("Doctor completes only own appointment that has already started")
    void doctorCompletesOwnStartedAppointment() {
        Appointment started = pastAppointment(doctor, LocalDateTime.now().minusHours(1), AppointmentStatus.SCHEDULED);

        AppointmentResponse completed = appointmentService.complete(started.getId(), doctorPrincipal);

        assertThat(completed.status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    @DisplayName("Completing a future appointment is rejected")
    void cannotCompleteFutureAppointment() {
        AppointmentResponse future = appointmentService.create(
                new AppointmentCreateRequest(doctor.getId(), patient.getId(), shortService.getId(),
                        workingDate.atTime(13, 0), null), adminPrincipal);

        assertThatThrownBy(() -> appointmentService.complete(future.id(), adminPrincipal))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("до его начала");
    }

    @Test
    @DisplayName("Doctor cannot complete or cancel another doctor's appointment")
    void doctorCannotManageForeignAppointment() {
        Appointment foreign = pastAppointment(secondDoctor, LocalDateTime.now().minusHours(1),
                AppointmentStatus.SCHEDULED);

        assertThatThrownBy(() -> appointmentService.complete(foreign.getId(), doctorPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> appointmentService.cancel(foreign.getId(),
                new CancelRequest("Не мой приём"), doctorPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Receptionist cannot complete an appointment")
    void receptionistCannotCompleteAppointment() {
        Appointment started = pastAppointment(doctor, LocalDateTime.now().minusHours(1),
                AppointmentStatus.SCHEDULED);

        assertThatThrownBy(() -> appointmentService.complete(started.getId(), receptionistPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("Patient deletion is blocked while appointments exist")
    void patientDeletionIsProtected() {
        appointmentService.create(new AppointmentCreateRequest(doctor.getId(), patient.getId(),
                shortService.getId(), workingDate.atTime(14, 0), null), adminPrincipal);

        assertThatThrownBy(() -> patientService.delete(patient.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("есть записи");

        Patient freePatient = patientRepository.save(new Patient("Свободный", "Пациент",
                LocalDate.of(1985, 1, 1), Gender.FEMALE, "+7 (916) 000-00-00", null, null));
        patientService.delete(freePatient.getId());
        assertThat(patientRepository.existsById(freePatient.getId())).isFalse();
    }

    @Test
    @DisplayName("Report aggregates counts, revenue and average check")
    void reportAggregatesClinicData() {
        appointmentRepository.save(appointmentAt(workingDate.atTime(9, 0),
                AppointmentStatus.COMPLETED, shortService.getPrice()));
        appointmentRepository.save(appointmentAt(workingDate.atTime(10, 0),
                AppointmentStatus.SCHEDULED, shortService.getPrice()));
        appointmentRepository.save(appointmentAt(workingDate.atTime(11, 0),
                AppointmentStatus.CANCELLED, shortService.getPrice()));

        ReportResponse report = reportService.buildReport(workingDate, workingDate);

        assertThat(report.summary().totalAppointments()).isEqualTo(3);
        assertThat(report.summary().completed()).isEqualTo(1);
        assertThat(report.summary().scheduled()).isEqualTo(1);
        assertThat(report.summary().cancelled()).isEqualTo(1);
        assertThat(report.summary().uniquePatients()).isEqualTo(1);
        assertThat(report.summary().revenue()).isEqualByComparingTo("2500.00");
        assertThat(report.summary().averageCheck()).isEqualByComparingTo("2500.00");
        assertThat(report.byDoctor()).hasSize(1);
        assertThat(report.byDoctor().get(0).revenue()).isEqualByComparingTo("2500.00");
        assertThat(report.daily()).hasSize(1);
        assertThat(report.daily().get(0).total()).isEqualTo(3);
    }

    private SlotResponse slotAt(AvailabilityResponse availability, int hour, int minute) {
        List<SlotResponse> slots = availability.slots();
        return slots.stream()
                .filter(slot -> slot.startTime().equals(workingDate.atTime(hour, minute)))
                .findFirst()
                .orElseThrow();
    }

    private Appointment pastAppointment(Doctor owner, LocalDateTime start, AppointmentStatus status) {
        Appointment appointment = new Appointment(owner, patient, shortService, start,
                start.plusMinutes(30), shortService.getPrice(), admin);
        appointment.setStatus(status);
        return appointmentRepository.saveAndFlush(appointment);
    }

    private Appointment appointmentAt(LocalDateTime start, AppointmentStatus status, BigDecimal price) {
        Appointment appointment = new Appointment(doctor, patient, shortService, start,
                start.plusMinutes(30), price, admin);
        appointment.setStatus(status);
        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(start.plusMinutes(30));
        }
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(start.minusHours(1));
        }
        return appointment;
    }
}
