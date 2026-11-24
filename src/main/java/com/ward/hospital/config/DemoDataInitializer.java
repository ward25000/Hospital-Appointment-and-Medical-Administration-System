package com.ward.hospital.config;

import com.ward.hospital.i18n.I18n;
import com.ward.hospital.util.Address;
import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.domain.User;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.DoctorScheduleRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.ReferralRepository;
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
import java.util.Comparator;
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
    private final ReferralRepository referralRepository;
    private final PasswordEncoder passwordEncoder;

    private final Map<Long, List<LocalDateTime[]>> bookedIntervals = new HashMap<>();

    public DemoDataInitializer(UserRepository userRepository,
                               SpecializationRepository specializationRepository,
                               DoctorRepository doctorRepository,
                               DoctorScheduleRepository scheduleRepository,
                               PatientRepository patientRepository,
                               MedicalServiceRepository serviceRepository,
                               AppointmentRepository appointmentRepository,
                               ReferralRepository referralRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.patientRepository = patientRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.referralRepository = referralRepository;
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
        List<MedicalService> services = seedServices(specializations);
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

        User patientUser = new User("patient.demo", passwordEncoder.encode("Patient123!"),
                "Соколова Анна", Role.PATIENT);
        userRepository.save(patientUser);
        patients.get(0).setUser(patientUser);

        Doctor ivanov = new Doctor(ivanovUser, specializations.get(0), "Алексей", "Иванов",
                "a.ivanov@medflow.kz", "+7 (727) 120-20-01", "201");
        ivanov.setMiddleName("Сергеевич");
        ivanov.setBioKk("Жоғары санатты кардиолог. Еңбек өтілі 18 жыл. Жүректің ишемиялық ауруын және ырғақ бұзылыстарын диагностикалауға маманданған.");
        ivanov.setBioEn("Cardiologist of the highest category with 18 years of experience. Specialises in coronary heart disease and rhythm disorders.");
        ivanov.setBio("Кардиолог высшей категории. Стаж 18 лет. Специализируется на диагностике "
                + "ишемической болезни сердца и нарушениях ритма.");
        Doctor petrova = new Doctor(petrovaUser, specializations.get(1), "Мария", "Петрова",
                "m.petrova@medflow.kz", "+7 (727) 120-20-02", "305");
        petrova.setMiddleName("Викторовна");
        petrova.setBioKk("Невролог, медицина ғылымдарының кандидаты. Бас сақинасы, нейропатия және жарақат салдары бар пациенттерді қабылдайды.");
        petrova.setBioEn("Neurologist, PhD. Treats migraine, neuropathies and the consequences of injuries.");
        petrova.setBio("Невролог, кандидат медицинских наук. Ведёт пациентов с мигренью, "
                + "нейропатиями и последствиями травм.");
        Doctor smirnov = new Doctor(smirnovUser, specializations.get(2), "Дмитрий", "Смирнов",
                "d.smirnov@medflow.kz", "+7 (727) 120-20-03", "102");
        smirnov.setMiddleName("Андреевич");
        smirnov.setBioKk("Педиатр. Балаларды туғаннан 18 жасқа дейін бақылайды, вакцинация және профилактикалық тексерулер бойынша кеңес береді.");
        smirnov.setBioEn("Pediatrician. Follows children from birth to 18, advises on vaccination and preventive check-ups.");
        smirnov.setBio("Педиатр. Наблюдает детей с рождения до 18 лет, консультирует по вопросам "
                + "вакцинации и профилактических осмотров.");
        Doctor kuznetsova = new Doctor(kuznetsovaUser, specializations.get(3), "Ольга", "Кузнецова",
                "o.kuznetsova@medflow.kz", "+7 (727) 120-20-04", "118");
        kuznetsova.setMiddleName("Игоревна");
        kuznetsova.setBioKk("Терапевт. Алғашқы диагностика, созылмалы ауруларды бақылау, профилактикалық бағдарламалар.");
        kuznetsova.setBioEn("Physician. Primary diagnosis, management of chronic diseases, prevention programmes.");
        kuznetsova.setBio("Терапевт. Первичная диагностика, ведение хронических заболеваний, "
                + "профилактические программы.");
        List<Doctor> doctors = doctorRepository.saveAll(List.of(ivanov, petrova, smirnov, kuznetsova));
        doctorRepository.flush();

        seedSchedules(doctors);

        seedAppointments(patients, services, doctors, admin);
        seedReferrals(patients, specializations, doctors, services);

        log.info("Demo data created: {} doctors, {} patients, {} services, {} appointments, {} referrals",
                doctors.size(), patients.size(), services.size(), appointmentRepository.count(),
                referralRepository.count());
    }

    private List<Specialization> seedSpecializations() {
        return specializationRepository.saveAll(List.of(
                new Specialization("Кардиология", "Диагностика и лечение заболеваний сердца и сосудов")
                        .translate("Кардиология", "Cardiology",
                                "Жүрек және қан тамырлары ауруларын диагностикалау және емдеу",
                                "Diagnosis and treatment of heart and vascular diseases"),
                new Specialization("Неврология", "Диагностика и лечение заболеваний нервной системы")
                        .translate("Неврология", "Neurology",
                                "Жүйке жүйесі ауруларын диагностикалау және емдеу",
                                "Diagnosis and treatment of nervous system diseases"),
                new Specialization("Педиатрия", "Медицинская помощь детям и подросткам")
                        .translate("Педиатрия", "Pediatrics",
                                "Балалар мен жасөспірімдерге медициналық көмек",
                                "Medical care for children and adolescents"),
                new Specialization("Терапия", "Первичная медицинская помощь и профилактика")
                        .translate("Терапия", "Internal medicine",
                                "Алғашқы медициналық көмек және профилактика",
                                "Primary care and prevention"),
                new Specialization("Ортопедия", "Заболевания и травмы опорно-двигательного аппарата")
                        .translate("Ортопедия", "Orthopedics",
                                "Тірек-қимыл аппаратының аурулары мен жарақаттары",
                                "Diseases and injuries of the musculoskeletal system")
        ));
    }

    private List<MedicalService> seedServices(List<Specialization> departments) {
        return serviceRepository.saveAll(List.of(
                new MedicalService("Первичная консультация врача",
                        "Осмотр, сбор анамнеза и план обследования", new BigDecimal("9000.00"), 30)
                        .translate("Дәрігердің алғашқы кеңесі", "Initial consultation",
                                "Қарап-тексеру, анамнез жинау және тексеру жоспары",
                                "Examination, history taking and a diagnostic plan"),
                new MedicalService("Повторная консультация",
                        "Контрольный приём и корректировка лечения", new BigDecimal("6000.00"), 30)
                        .translate("Қайта кеңес алу", "Follow-up consultation",
                                "Бақылау қабылдауы және емді түзету",
                                "Follow-up visit and treatment adjustment"),
                new MedicalService("ЭКГ",
                        "Электрокардиография в 12 отведениях с расшифровкой", new BigDecimal("4500.00"), 30)
                        .translate("ЭКГ", "ECG",
                                "12 тармақтағы электрокардиография, қорытындысымен",
                                "12-lead electrocardiography with interpretation")
                        .asDiagnostic(departments.get(0), "210"),
                new MedicalService("УЗИ сердца",
                        "Эхокардиография с допплерографией", new BigDecimal("14000.00"), 45)
                        .translate("Жүректің УДЗ", "Echocardiography",
                                "Допплерографиямен эхокардиография",
                                "Heart ultrasound with Doppler imaging")
                        .asDiagnostic(departments.get(0), "212"),
                new MedicalService("Общий анализ крови",
                        "Клинический анализ крови с лейкоцитарной формулой", new BigDecimal("2800.00"), 15)
                        .translate("Жалпы қан талдауы", "Complete blood count",
                                "Лейкоцитарлық формуласымен клиникалық қан талдауы",
                                "Clinical blood test with white cell differential")
                        .asDiagnostic(departments.get(3), "115"),
                new MedicalService("Неврологический осмотр",
                        "Оценка рефлексов, чувствительности и неврологического статуса",
                        new BigDecimal("8000.00"), 30)
                        .translate("Неврологиялық тексеру", "Neurological examination",
                                "Рефлекстерді, сезімталдықты және неврологиялық мәртебені бағалау",
                                "Assessment of reflexes, sensitivity and neurological status"),
                new MedicalService("Детский профилактический осмотр",
                        "Комплексный осмотр ребёнка и рекомендации по развитию", new BigDecimal("7500.00"), 30)
                        .translate("Балаларды профилактикалық тексеру", "Child preventive check-up",
                                "Баланы кешенді тексеру және даму бойынша ұсыныстар",
                                "Comprehensive examination of a child and development advice"),
                new MedicalService("Внутрисуставная инъекция",
                        "Лечебная инъекция под контролем специалиста", new BigDecimal("12000.00"), 45)
                        .translate("Буынішілік инъекция", "Intra-articular injection",
                                "Маманның бақылауымен емдік инъекция",
                                "Therapeutic injection under specialist control")
        ));
    }

    private List<Patient> seedPatients() {
        return patientRepository.saveAll(List.of(
                patient("900412400015", "Анна", "Соколова", LocalDate.of(1990, 4, 12), Gender.FEMALE,
                        "+7 (701) 100-10-01", "a.sokolova@example.com", "ул. Абая", "10", "4"),
                patient("850923300027", "Михаил", "Орлов", LocalDate.of(1985, 9, 23), Gender.MALE,
                        "+7 (701) 100-10-02", "m.orlov@example.com", "ул. Толе би", "24", "8"),
                patient("780130400038", "Елена", "Волкова", LocalDate.of(1978, 1, 30), Gender.FEMALE,
                        "+7 (701) 100-10-03", "e.volkova@example.com", "пр. Назарбаева", "7", null),
                patient("980617300049", "Артём", "Лебедев", LocalDate.of(1998, 6, 17), Gender.MALE,
                        "+7 (701) 100-10-04", "a.lebedev@example.com", "ул. Сатпаева", "41", "16"),
                patient("921205400050", "Ольга", "Морозова", LocalDate.of(1992, 12, 5), Gender.FEMALE,
                        "+7 (701) 100-10-05", "o.morozova@example.com", "ул. Жандосова", "15", "20"),
                patient("690314300061", "Сергей", "Новиков", LocalDate.of(1969, 3, 14), Gender.MALE,
                        "+7 (701) 100-10-06", "s.novikov@example.com", "ул. Тимирязева", "8", null),
                patient("010721600072", "Дарья", "Зайцева", LocalDate.of(2001, 7, 21), Gender.FEMALE,
                        "+7 (701) 100-10-07", "d.zaitseva@example.com", "пр. Достык", "19", "28"),
                patient("881102300083", "Павел", "Егоров", LocalDate.of(1988, 11, 2), Gender.MALE,
                        "+7 (701) 100-10-08", "p.egorov@example.com", "ул. Розыбакиева", "33", "32"),
                patient("750509400094", "Ирина", "Павлова", LocalDate.of(1975, 5, 9), Gender.FEMALE,
                        "+7 (701) 100-10-09", "i.pavlova@example.com", "ул. Байтурсынова", "5", null),
                patient("140819500105", "Никита", "Козлов", LocalDate.of(2014, 8, 19), Gender.MALE,
                        "+7 (701) 100-10-10", "n.kozlov@example.com", "ул. Гоголя", "12", "40"),
                patient("180227600116", "София", "Белова", LocalDate.of(2018, 2, 27), Gender.FEMALE,
                        "+7 (701) 100-10-11", "s.belova@example.com", "ул. Кабанбай батыра", "3", "44"),
                patient("621011300127", "Виктор", "Громов", LocalDate.of(1962, 10, 11), Gender.MALE,
                        "+7 (701) 100-10-12", "v.gromov@example.com", "пр. Аль-Фараби", "21", null)
        ));
    }

    private Patient patient(String iin, String firstName, String lastName, LocalDate birthDate, Gender gender,
                            String phone, String email, String street, String house, String apartment) {
        Address address = Address.of("Алматы", street, house, apartment);
        Patient patient = new Patient(iin, firstName, lastName, birthDate, gender, phone, email,
                address.format(I18n.RU));
        patient.applyAddress(address);
        return patient;
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
        addAppointment(today.minusDays(1), LocalTime.of(12, 30), 2, 1, patients, services, doctors, admin,
                AppointmentStatus.NO_SHOW, "Пациент не явился на приём", null);

        LocalDate friday = today.minusDays(today.getDayOfWeek().getValue() + 2L);
        Appointment finished = addAppointment(friday, LocalTime.of(9, 0), 0, 0, patients, services, doctors, admin,
                AppointmentStatus.COMPLETED, "Жалобы на головную боль и повышение давления", null);
        finished.setDiagnosis("I10 Артериальная гипертензия, I степень");
        finished.setConclusion("АД 145/90 мм рт. ст., пульс 78 в минуту. Тоны сердца ясные, ритм правильный. "
                + "Очаговой неврологической симптоматики нет.");
        finished.setRecommendations("Контроль АД два раза в день, ограничение соли, консультация невролога.");
        // The visit is over, but the doctor has not marked whether the patient attended.
        addAppointment(friday, LocalTime.of(16, 0), 8, 1, patients, services, doctors, admin,
                AppointmentStatus.SCHEDULED, "Контрольный приём", null);
    }

    private void seedReferrals(List<Patient> patients, List<Specialization> specializations,
                               List<Doctor> doctors, List<MedicalService> services) {
        Doctor ivanov = doctors.get(0);
        Doctor petrova = doctors.get(1);
        Doctor kuznetsova = doctors.get(3);

        Referral toNeurologist = new Referral(patients.get(0), ivanov,
                findAppointment(patients.get(0)));
        toNeurologist.setToDoctor(petrova);
        toNeurologist.setService(services.get(5));
        toNeurologist.setPrice(services.get(5).getPrice());
        toNeurologist.setDepartment(petrova.getSpecialization().getName());
        toNeurologist.setOffice(petrova.getOffice());
        toNeurologist.setReason("Головные боли, направление к неврологу");

        Referral toCardiologist = new Referral(patients.get(1), kuznetsova,
                findAppointment(patients.get(1)));
        toCardiologist.setToSpecialization(specializations.get(0));
        toCardiologist.setDepartment(specializations.get(0).getName());
        toCardiologist.setService(services.get(2));
        toCardiologist.setPrice(services.get(2).getPrice());
        toCardiologist.setOffice(services.get(2).getOffice());
        toCardiologist.setReason("Повышенное артериальное давление, нужна консультация кардиолога");

        referralRepository.saveAll(List.of(toNeurologist, toCardiologist));
    }

    private Appointment findAppointment(Patient patient) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getPatient().getId().equals(patient.getId()))
                .sorted(Comparator.comparing(Appointment::getStartTime))
                .findFirst()
                .orElse(null);
    }

    private Appointment addAppointment(LocalDate date, LocalTime time, int patientIndex, int serviceIndex,
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
            return null;
        }

        Appointment appointment = new Appointment(doctor, patients.get(patientIndex), service,
                start, end, service.getPrice(), admin);
        appointment.setStatus(status);
        appointment.setNotes(notes);
        if (status == AppointmentStatus.COMPLETED) {
            appointment.setCompletedAt(end);
            appointment.setDiagnosis("Z00.0 Общий медицинский осмотр");
            appointment.setConclusion("Состояние удовлетворительное. Жалоб на момент осмотра нет, "
                    + "показатели в пределах возрастной нормы.");
            appointment.setRecommendations("Соблюдать режим труда и отдыха, повторный осмотр через 6 месяцев.");
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
        return appointment;
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
