package com.ward.hospital.controller;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.domain.User;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Patient portal and referral workflow integration tests")
class PatientPortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private SpecializationRepository specializationRepository;
    @Autowired
    private MedicalServiceRepository medicalServiceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    private Patient patient;
    private Patient otherPatient;
    private Doctor doctor;
    private Doctor neurologist;
    private Appointment pastVisit;
    private Appointment activeVisit;
    private UserPrincipal patientPrincipal;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal neurologistPrincipal;
    private Specialization neurology;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Specialization cardiology = specializationRepository.save(
                new Specialization("Кардиология " + suffix, "Сердце"));
        neurology = specializationRepository.save(new Specialization("Неврология " + suffix, "Нервы"));

        User admin = userRepository.save(new User("admin-" + suffix, "$2a$10$test",
                "Администратор", Role.ADMIN));
        User patientUser = userRepository.save(new User("patient-" + suffix, "$2a$10$test",
                "Иван Иванов", Role.PATIENT));
        User doctorUser = userRepository.save(new User("doctor-" + suffix, "$2a$10$test",
                "Иван Врач", Role.DOCTOR));
        User neuroUser = userRepository.save(new User("neuro-" + suffix, "$2a$10$test",
                "Пётр Невролог", Role.DOCTOR));

        patient = patientRepository.save(new Patient("111111111111", "Иван", "Иванов",
                LocalDate.of(1990, 1, 1), Gender.MALE, "+7 (916) 111-11-11",
                "ivan@example.com", "Москва, ул. Лесная, 1"));
        patient.setUser(patientUser);
        otherPatient = patientRepository.save(new Patient("222222222222", "Мария", "Петрова",
                LocalDate.of(1988, 2, 2), Gender.FEMALE, "+7 (916) 222-22-22",
                "maria@example.com", "Москва, ул. Садовая, 2"));

        doctor = doctorRepository.save(new Doctor(doctorUser, cardiology, "Иван", "Врач",
                "doctor-" + suffix + "@example.com", "+7 (900) 000-00-01", "201"));
        neurologist = doctorRepository.save(new Doctor(neuroUser, neurology, "Пётр", "Невролог",
                "neuro-" + suffix + "@example.com", "+7 (900) 000-00-02", "305"));

        MedicalService service = medicalServiceRepository.save(new MedicalService(
                "Консультация " + suffix, "Приём", new BigDecimal("2500.00"), 30));

        pastVisit = appointmentRepository.save(new Appointment(doctor, patient, service,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1),
                service.getPrice(), admin));
        pastVisit.setStatus(AppointmentStatus.SCHEDULED);

        activeVisit = appointmentRepository.save(new Appointment(doctor, patient, service,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusMinutes(30),
                service.getPrice(), admin));
        activeVisit.setStatus(AppointmentStatus.SCHEDULED);

        appointmentRepository.save(new Appointment(neurologist, otherPatient, service,
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusMinutes(30),
                service.getPrice(), admin));

        patientPrincipal = new UserPrincipal(patientUser.getId(), patientUser.getUsername(),
                "unused", patientUser.getFullName(), Role.PATIENT, null, patient.getId(), true);
        doctorPrincipal = new UserPrincipal(doctorUser.getId(), doctorUser.getUsername(),
                "unused", doctorUser.getFullName(), Role.DOCTOR, doctor.getId(), true);
        neurologistPrincipal = new UserPrincipal(neuroUser.getId(), neuroUser.getUsername(),
                "unused", neuroUser.getFullName(), Role.DOCTOR, neurologist.getId(), true);
    }

    @Test
    @DisplayName("Patient can self-register; duplicate IIN is rejected")
    void registersPatientAndRejectsDuplicateIin() throws Exception {
        String registration = """
                {"username":"new.patient","password":"Patient123!","iin":"333333333333",
                 "firstName":"Анна","lastName":"Соколова","birthDate":"1991-03-04","gender":"FEMALE",
                 "phone":"+7 (916) 333-33-33","email":"anna@example.com",
                 "address":"Москва, ул. Новая, 3"}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.patientId").isNumber());

        assertThat(patientRepository.findByIin("333333333333")).isPresent();

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Patient reads and updates only their own profile")
    void patientManagesOwnProfile() throws Exception {
        mockMvc.perform(get("/api/me/profile").with(user(patientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iin").value("111111111111"))
                .andExpect(jsonPath("$.address").value("Москва, ул. Лесная, 1"));

        mockMvc.perform(put("/api/me/profile").with(user(patientPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Иван","lastName":"Иванов",
                                 "birthDate":"1990-01-01","gender":"MALE",
                                 "phone":"+7 (916) 999-99-99","email":"ivan@example.com",
                                 "address":"Москва, ул. Обновлённая, 7"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Москва, ул. Обновлённая, 7"));

        mockMvc.perform(get("/api/patients").with(user(patientPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Patient visit history contains own visits with doctor and specialty")
    void patientSeesOwnVisitHistory() throws Exception {
        mockMvc.perform(get("/api/me/visits").with(user(patientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorName").exists())
                .andExpect(jsonPath("$[0].doctorSpecialization").exists())
                .andExpect(jsonPath("$[0].statusLabel").value("Запланирован"));

        mockMvc.perform(get("/api/appointments").with(user(patientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patient.getId()));

        mockMvc.perform(get("/api/appointments/" + activeVisit.getId()).with(user(patientPrincipal)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doctor marks a past visit as no-show")
    void doctorMarksNoShow() throws Exception {
        mockMvc.perform(post("/api/appointments/" + pastVisit.getId() + "/no-show")
                        .with(user(doctorPrincipal)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_SHOW"))
                .andExpect(jsonPath("$.statusLabel").value("Не явился"));

        mockMvc.perform(post("/api/appointments/" + activeVisit.getId() + "/no-show")
                        .with(user(doctorPrincipal)).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Referral is routed to a specific doctor and only they see it")
    void referralRoutedToSpecificDoctor() throws Exception {
        String request = """
                {"appointmentId":%d,"toDoctorId":%d,"reason":"Нужна консультация невролога"}
                """.formatted(activeVisit.getId(), neurologist.getId());

        mockMvc.perform(post("/api/referrals").with(user(doctorPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.toDoctorId").value(neurologist.getId()))
                .andExpect(jsonPath("$.department").value(neurology.getName()))
                .andExpect(jsonPath("$.office").value("305"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/referrals/incoming").with(user(neurologistPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(patient.getId()))
                .andExpect(jsonPath("$[0].fromDoctorId").value(doctor.getId()));

        mockMvc.perform(get("/api/referrals/incoming").with(user(doctorPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    @DisplayName("Referral by specialty is visible to every doctor of that specialty")
    void referralRoutedBySpecialty() throws Exception {
        String request = """
                {"appointmentId":%d,"toSpecializationId":%d,"department":"Неврология",
                 "office":"305","reason":"Направление по специальности"}
                """.formatted(activeVisit.getId(), neurology.getId());

        mockMvc.perform(post("/api/referrals").with(user(doctorPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.toDoctorId").doesNotExist())
                .andExpect(jsonPath("$.toSpecializationId").value(neurology.getId()));

        mockMvc.perform(get("/api/referrals/incoming").with(user(neurologistPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toSpecializationName").value(neurology.getName()));
    }

    @Test
    @DisplayName("Recipient doctor updates referral status; patient sees the referral")
    void referralStatusWorkflow() throws Exception {
        String request = """
                {"appointmentId":%d,"toDoctorId":%d,"reason":"Консультация"}
                """.formatted(activeVisit.getId(), neurologist.getId());

        String response = mockMvc.perform(post("/api/referrals").with(user(doctorPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long referralId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        mockMvc.perform(put("/api/referrals/" + referralId + "/status")
                        .with(user(neurologistPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedAt").exists());

        mockMvc.perform(put("/api/referrals/" + referralId + "/status")
                        .with(user(neurologistPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(put("/api/referrals/" + referralId + "/status")
                        .with(user(neurologistPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/referrals/mine").with(user(patientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(referralId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Doctor cannot create a referral for somebody else's appointment")
    void referralForForeignAppointmentIsRejected() throws Exception {
        String request = """
                {"appointmentId":%d,"toDoctorId":%d,"reason":"Консультация"}
                """.formatted(activeVisit.getId(), neurologist.getId());

        mockMvc.perform(post("/api/referrals").with(user(neurologistPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isForbidden());
    }
}
