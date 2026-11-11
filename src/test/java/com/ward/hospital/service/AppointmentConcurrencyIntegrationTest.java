package com.ward.hospital.service;

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
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Double-booking prevention under concurrent requests")
class AppointmentConcurrencyIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private LocalDate workingDate;
    private Doctor doctor;
    private Patient patient;
    private MedicalService service;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        workingDate = LocalDate.now().plusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        Specialization specialization = specializationRepository.save(
                new Specialization("Ортопедия " + suffix, "Тестовое направление"));
        User admin = userRepository.save(new User("admin-" + suffix, "$2a$10$test",
                "Администратор Тестов", Role.ADMIN));
        User doctorUser = userRepository.save(new User("doctor-" + suffix, "$2a$10$test",
                "Иван Врач", Role.DOCTOR));
        doctor = doctorRepository.save(new Doctor(doctorUser, specialization, "Иван", "Врач",
                "doctor-" + suffix + "@example.com", "+7 (900) 000-00-01", "101"));
        scheduleRepository.save(new DoctorSchedule(doctor, workingDate.getDayOfWeek(),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 30));
        patient = patientRepository.save(new Patient("Пациент", "Конкурентный",
                LocalDate.of(1990, 5, 15), Gender.MALE, "+7 (916) 555-44-33", null, null));
        service = medicalServiceRepository.save(new MedicalService(
                "Консультация " + suffix, null, new BigDecimal("2500.00"), 30));
        adminPrincipal = UserPrincipal.from(admin, null);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("delete from appointments");
        jdbcTemplate.execute("delete from doctor_schedules");
        jdbcTemplate.execute("delete from doctors");
        jdbcTemplate.execute("delete from patients");
        jdbcTemplate.execute("delete from medical_services");
        jdbcTemplate.execute("delete from specializations");
        jdbcTemplate.execute("delete from users");
    }

    @Test
    @DisplayName("Two simultaneous bookings for the same slot produce exactly one appointment")
    void onlyOneConcurrentBookingWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        try {
            List<Future<?>> futures = List.of(
                    pool.submit(bookingTask(ready, start, successes, conflicts, unexpected)),
                    pool.submit(bookingTask(ready, start, successes, conflicts, unexpected))
            );
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(unexpected.get()).isZero();
        assertThat(appointmentRepository.countByDoctorId(doctor.getId())).isEqualTo(1);
    }

    private Runnable bookingTask(CountDownLatch ready, CountDownLatch start,
                                 AtomicInteger successes, AtomicInteger conflicts,
                                 AtomicInteger unexpected) {
        return () -> {
            ready.countDown();
            try {
                start.await(10, TimeUnit.SECONDS);
                AppointmentResponse response = appointmentService.create(
                        new AppointmentCreateRequest(doctor.getId(), patient.getId(), service.getId(),
                                workingDate.atTime(10, 0), "Одновременная запись"),
                        adminPrincipal);
                if (response.id() != null) {
                    successes.incrementAndGet();
                }
            } catch (ConflictException ex) {
                conflicts.incrementAndGet();
            } catch (Throwable ex) {
                unexpected.incrementAndGet();
            }
        };
    }
}
