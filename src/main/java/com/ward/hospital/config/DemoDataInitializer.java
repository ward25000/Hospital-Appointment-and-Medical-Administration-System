package com.ward.hospital.config;

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
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PatientRepository patientRepository;
    private final MedicalServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<Long, List<LocalDateTime[]>> bookedIntervals = new HashMap<>();

    public DemoDataInitializer(UserRepository userRepository,
                               SpecializationRepository specializationRepository,
                               DoctorRepository doctorRepository,
                               DoctorScheduleRepository scheduleRepository,
                               PatientRepository patientRepository,
                               MedicalServiceRepository serviceRepository,
                               AppointmentRepository appointmentRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.patientRepository = patientRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present — seeding skipped");
            return;
        }

        List<Specialization> specializations = seedSpecializations();
        List<MedicalService> services = seedServices();
        List<Patient> patients = seedPatients();

        List<User> staff = new ArrayList<>();
        User admin = new User("admin", passwordEncoder.encode("Admin123!"),
                "Анна Викторовна Смирнова", Role.ADMIN);
        User receptionist = new User("reception", passwordEncoder.encode("Reception123!"),
                "Ирина Александровна Ковалёва", Role.RECEPTIONIST);
        staff.add(admin);
        staff.add(receptionist);

        User ivanovUser = new User("doctor.ivanov", passwordEncoder.encode("Doctor123!"),
                "Иванов Алексей Сергеевич", Role.DOCTOR);
        User petrovaUser = new User("doctor.petrova", passwordEncoder.encode("Doctor123!"),
                "Петрова Мария Викторовна", Role.DOCTOR);
        User smirnovUser = new User("doctor.smirnov", passwordEncoder.encode("Doctor123!"),
                "Смирнов Дмитрий Андреевич", Role.DOCTOR);
        User kuznetsovaUser = new User("doctor.kuznetsova", passwordEncoder.encode("Doctor123!"),
                "Кузнецова Ольга Игоревна", Role.DOCTOR);
        staff.addAll(List.of(ivanovUser, petrovaUser, smirnovUser, kuznetsovaUser));
        userRepository.saveAll(staff);

        Doctor ivanov = new Doctor(ivanovUser, specializations.get(0), "Алексей", "Иванов",
                "a.ivanov@medcenter.ru", "+7 (495) 120-20-01", "201");
        ivanov.setBio("Кардиолог высшей категории. Стаж 18 лет. Специализируется на диагностике "
                + "ишемической болезни сердца и нарушениях ритма.");
        Doctor petrova = new Doctor(petrovaUser, specializations.get(1), "Мария", "Петрова",
                "m.petrova@medcenter.ru", "+7 (495) 120-20-02", "305");
        petrova.setBio("Невролог, кандидат медицинских наук. Ведёт пациентов с мигренью, "
                + "нейропатиями и последствиями травм.");
        Doctor smirnov = new Doctor(smirnovUser, specializations.get(2), "Дмитрий", "Смирнов",
                "d.smirnov@medcenter.ru", "+7 (495) 120-20-03", "102");
        smirnov.setBio("Педиатр. Наблюдает детей с рождения до 18 лет, консультирует по вопросам "
                + "вакцинации и профилактических осмотров.");
        Doctor kuznetsova = new Doctor(kuznetsovaUser, specializations.get(3), "Ольга", "Кузнецова",
                "o.kuznetsova@medcenter.ru", "+7 (495) 120-20-04", "118");
        kuznetsova.setBio("Терапевт. Первичная диагностика, ведение хронических заболеваний, "
                + "профилактические программы.");
        List<Doctor> doctors = doctorRepository.saveAll(List.of(ivanov, petrova, smirnov, kuznetsova));
        doctorRepository.flush();

        seedSchedules(doctors);

        seedAppointments(patients, services, doctors, admin);

        log.info("Demo data created: {} doctors, {} patients, {} services, {} appointments",
                doctors.size(), patients.size(), services.size(), appointmentRepository.count());
    }

    private List<Specialization> seedSpecializations() {
        return specializationRepository.saveAll(List.of(
                new Specialization("Кардиология", "Диагностика и лечение заболеваний сердца и сосудов"),
                new Specialization("Неврология", "Диагностика и лечение заболеваний нервной системы"),
                new Specialization("Педиатрия", "Медицинская помощь детям и подросткам"),
                new Specialization("Терапия", "Первичная медицинская помощь и профилактика"),
                new Specialization("Ортопедия", "Заболевания и травмы опорно-двигательного аппарата")
        ));
    }

    private List<MedicalService> seedServices() {
        return serviceRepository.saveAll(List.of(
                new MedicalService("Первичная консультация врача",
                        "Осмотр, сбор анамнеза и план обследования", new BigDecimal("2500.00"), 30),
                new MedicalService("Повторная консультация",
                        "Контрольный приём и корректировка лечения", new BigDecimal("1800.00"), 30),
                new MedicalService("ЭКГ",
                        "Электрокардиография в 12 отведениях с расшифровкой", new BigDecimal("1200.00"), 30),
                new MedicalService("УЗИ сердца",
                        "Эхокардиография с допплерографией", new BigDecimal("3200.00"), 45),
                new MedicalService("Общий анализ крови",
                        "Клинический анализ крови с лейкоцитарной формулой", new BigDecimal("900.00"), 15),
                new MedicalService("Неврологический осмотр",
                        "Оценка рефлексов, чувствительности и неврологического статуса",
                        new BigDecimal("2200.00"), 30),
                new MedicalService("Детский профилактический осмотр",
                        "Комплексный осмотр ребёнка и рекомендации по развитию", new BigDecimal("2000.00"), 30),
                new MedicalService("Внутрисуставная инъекция",
                        "Лечебная инъекция под контролем специалиста", new BigDecimal("3500.00"), 45)
        ));
    }

    private List<Patient> seedPatients() {
        return patientRepository.saveAll(List.of(
                new Patient("Анна", "Соколова", LocalDate.of(1990, 4, 12), Gender.FEMALE,
                        "+7 (916) 100-10-01", "a.sokolova@example.com", "Москва, ул. Лесная, 10"),
                new Patient("Михаил", "Орлов", LocalDate.of(1985, 9, 23), Gender.MALE,
                        "+7 (916) 100-10-02", "m.orlov@example.com", "Москва, ул. Мира, 24"),
                new Patient("Елена", "Волкова", LocalDate.of(1978, 1, 30), Gender.FEMALE,
                        "+7 (916) 100-10-03", "e.volkova@example.com", "Москва, ул. Садовая, 7"),
                new Patient("Артём", "Лебедев", LocalDate.of(1998, 6, 17), Gender.MALE,
                        "+7 (916) 100-10-04", "a.lebedev@example.com", "Москва, пр-т Вернадского, 41"),
                new Patient("Ольга", "Морозова", LocalDate.of(1992, 12, 5), Gender.FEMALE,
                        "+7 (916) 100-10-05", "o.morozova@example.com", "Москва, ул. Полярная, 15"),
                new Patient("Сергей", "Новиков", LocalDate.of(1969, 3, 14), Gender.MALE,
                        "+7 (916) 100-10-06", "s.novikov@example.com", "Москва, ул. Академика Королёва, 8"),
                new Patient("Дарья", "Зайцева", LocalDate.of(2001, 7, 21), Gender.FEMALE,
                        "+7 (916) 100-10-07", "d.zaitseva@example.com", "Москва, ул. Тверская, 19"),
                new Patient("Павел", "Егоров", LocalDate.of(1988, 11, 2), Gender.MALE,
                        "+7 (916) 100-10-08", "p.egorov@example.com", "Москва, ул. Гагарина, 33"),
                new Patient("Ирина", "Павлова", LocalDate.of(1975, 5, 9), Gender.FEMALE,
                        "+7 (916) 100-10-09", "i.pavlova@example.com", "Москва, ул. Никольская, 5"),
                new Patient("Никита", "Козлов", LocalDate.of(2014, 8, 19), Gender.MALE,
                        "+7 (916) 100-10-10", "n.kozlov@example.com", "Москва, ул. Строителей, 12"),
                new Patient("София", "Белова", LocalDate.of(2018, 2, 27), Gender.FEMALE,
                        "+7 (916) 100-10-11", "s.belova@example.com", "Москва, ул. Молодёжная, 3"),
                new Patient("Виктор", "Громов", LocalDate.of(1962, 10, 11), Gender.MALE,
                        "+7 (916) 100-10-12", "v.gromov@example.com", "Москва, ул. Ветеранская, 21")
        ));
    }

    private void seedSchedules(List<Doctor> doctors) {
        Doctor ivanov = doctors.get(0);
        Doctor petrova = doctors.get(1);
        Doctor smirnov = doctors.get(2);
        Doctor kuznetsova = doctors.get(3);

        addSchedules(ivanov, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                LocalTime.of(9, 0), LocalTime.of(17, 0));
        addSchedules(petrova, EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
                LocalTime.of(8, 0), LocalTime.of(14, 0));
        addSchedules(smirnov, EnumSet.complementOf(EnumSet.of(DayOfWeek.MONDAY)),
                LocalTime.of(10, 0), LocalTime.of(18, 0));
        addSchedules(kuznetsova, EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                LocalTime.of(8, 0), LocalTime.of(16, 0));
    }

    private void addSchedules(Doctor doctor, Set<DayOfWeek> days, LocalTime start, LocalTime end) {
        List<DoctorSchedule> schedules = days.stream()
                .map(day -> new DoctorSchedule(doctor, day, start, end, 30))
                .toList();
        scheduleRepository.saveAll(schedules);
    }

    private void seedAppointments(List<Patient> patients, List<MedicalService> services,
                                  List<Doctor> doctors, User admin) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 4; i++) {
            LocalTime time = LocalTime.of(9 + i * 2, i == 1 ? 30 : 0);
            AppointmentStatus status = LocalDateTime.of(today, time).isBefore(now)
                    ? AppointmentStatus.COMPLETED : AppointmentStatus.SCHEDULED;
            addAppointment(today, time, i, i + 1, patients, services, doctors, admin, status,
                    "Плановый приём", null);
        }

        int[] pastOffsets = {-2, -4, -6, -9, -12, -16};
        LocalTime[] pastTimes = {LocalTime.of(10, 0), LocalTime.of(11, 30), LocalTime.of(15, 0),
                LocalTime.of(9, 30), LocalTime.of(13, 0), LocalTime.of(14, 0)};
        for (int i = 0; i < pastOffsets.length; i++) {
            addAppointment(today.plusDays(pastOffsets[i]), pastTimes[i], i + 2, (i + 2) % services.size(),
                    patients, services, doctors, admin, AppointmentStatus.COMPLETED,
                    "Приём завершён, выданы рекомендации", null);
        }

        int[] futureOffsets = {1, 2, 3, 5, 7, 10};
        LocalTime[] futureTimes = {LocalTime.of(10, 0), LocalTime.of(11, 30), LocalTime.of(14, 0),
                LocalTime.of(15, 30), LocalTime.of(9, 30), LocalTime.of(12, 30)};
        for (int i = 0; i < futureOffsets.length; i++) {
            addAppointment(today.plusDays(futureOffsets[i]), futureTimes[i], i,
                    (i + 3) % services.size(), patients, services, doctors, admin,
                    AppointmentStatus.SCHEDULED, "Запись создана регистратурой", null);
        }

        addAppointment(today.minusDays(3), LocalTime.of(12, 0), 6, 2, patients, services, doctors, admin,
                AppointmentStatus.CANCELLED, "Пациент перенёс визит", "Перенос по инициативе пациента");
        addAppointment(today.plusDays(4), LocalTime.of(13, 0), 7, 4, patients, services, doctors, admin,
                AppointmentStatus.CANCELLED, null, "Пациент отменил запись");
    }

    private void addAppointment(LocalDate date, LocalTime time, int patientIndex, int serviceIndex,
                                List<Patient> patients, List<MedicalService> services,
                                List<Doctor> doctors, User admin, AppointmentStatus status,
                                String notes, String cancellationReason) {
        MedicalService service = services.get(serviceIndex);
        LocalDateTime start = LocalDateTime.of(date, time);
        LocalDateTime end = start.plusMinutes(service.getDurationMinutes());

        Doctor doctor = doctors.stream()
                .filter(candidate -> isWorking(candidate, date, time, end))
                .filter(candidate -> isFree(candidate, start, end))
                .findFirst()
                .orElse(null);
        if (doctor == null) {
            log.debug("No free doctor for demo appointment at {}", start);
            return;
        }

        Appointment appointment = new Appointment(doctor, patients.get(patientIndex), service,
                start, end, service.getPrice(), admin);
        appointment.setStatus(status);
        appointment.setNotes(notes);
        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(end);
        }
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancelledAt(LocalDateTime.now());
            appointment.setCancellationReason(cancellationReason);
        }

        appointmentRepository.save(appointment);
        if (status != AppointmentStatus.CANCELLED) {
            bookedIntervals.computeIfAbsent(doctor.getId(), key -> new ArrayList<>())
                    .add(new LocalDateTime[]{start, end});
        }
    }

    private boolean isWorking(Doctor doctor, LocalDate date, LocalTime start, LocalDateTime end) {
        return scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctor.getId(), date.getDayOfWeek())
                .map(schedule -> !start.isBefore(schedule.getStartTime())
                        && !end.toLocalTime().isAfter(schedule.getEndTime()))
                .orElse(false);
    }

    private boolean isFree(Doctor doctor, LocalDateTime start, LocalDateTime end) {
        return bookedIntervals.getOrDefault(doctor.getId(), List.of()).stream()
                .noneMatch(interval -> start.isBefore(interval[1]) && end.isAfter(interval[0]));
    }
}
