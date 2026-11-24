package com.ward.hospital.controller;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Gender;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.ReceiptEmailStatus;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.domain.User;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.PaymentRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Not transactional on purpose: the receipt is emailed only after a real commit. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("MIS terms of reference: cards, visits, payments, documents, languages")
class MisWorkflowIntegrationTest {

    private static final String CARD = """
            {"number":"%s","expiry":"12/40","cvc":"123","holder":"TEST"}""";
    private static final String CONCLUSION = """
            {"diagnosis":"I10 Гипертензия","conclusion":"Состояние стабильное","recommendations":"Контроль АД"}""";

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
    private MedicalServiceRepository serviceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    private Patient patient;
    private Appointment visit;
    private MedicalService ecg;
    private UserPrincipal patientPrincipal;
    private UserPrincipal strangerPrincipal;
    private UserPrincipal doctorPrincipal;
    private UserPrincipal cardiologistPrincipal;
    private UserPrincipal receptionPrincipal;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L));
        Specialization therapy = specializationRepository.save(new Specialization("Терапия " + suffix, null)
                .translate("Терапия KK " + suffix, "Internal medicine " + suffix, null, null));
        Specialization cardiology = specializationRepository.save(new Specialization("Кардиология " + suffix, null)
                .translate("Кардиология KK " + suffix, "Cardiology " + suffix, null, null));
        MedicalService consultation = serviceRepository.save(new MedicalService("Консультация " + suffix, null,
                new BigDecimal("9000.00"), 30).translate("Кеңес беру", "Consultation", null, null));
        ecg = serviceRepository.save(new MedicalService("ЭКГ " + suffix, null, new BigDecimal("4500.00"), 30)
                .translate("ЭКГ", "ECG", null, null).asDiagnostic(cardiology, "210"));

        patient = patientRepository.save(card(iin(), "Соколова", "+7 (701) 100-10-01", "anna@example.com"));
        User patientUser = userRepository.save(new User("p-" + suffix, "x", patient.getFullName(), Role.PATIENT));
        patient.setUser(patientUser);
        patient = patientRepository.save(patient);
        Doctor doctor = doctor("d1-" + suffix, therapy, "118");
        Doctor cardiologist = doctor("d2-" + suffix, cardiology, "201");
        User reception = userRepository.save(new User("r-" + suffix, "x", "Регистратор", Role.RECEPTIONIST));

        visit = appointmentRepository.save(new Appointment(doctor, patient, consultation,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(30),
                consultation.getPrice(), reception));

        patientPrincipal = UserPrincipal.from(patientUser, null, patient.getId());
        strangerPrincipal = new UserPrincipal(patientUser.getId(), "stranger", "x", "x", Role.PATIENT, null, -1L, true);
        doctorPrincipal = UserPrincipal.from(doctor.getUser(), doctor.getId());
        cardiologistPrincipal = UserPrincipal.from(cardiologist.getUser(), cardiologist.getId());
        receptionPrincipal = UserPrincipal.from(reception, null);
    }

    @Test
    @DisplayName("A card opened by the clinic is found by IIN and linked, no duplicate appears")
    void linksExistingCard() throws Exception {
        Patient card = patientRepository.save(card(iin(), "Ахметова", "+7 (702) 555-44-33", "card@example.com"));
        lookup(card.getIin(), "CARD_FOUND");

        register(card.getIin(), "Ахметова", "+7 702 000 00 00", "other@example.com")
                .andExpect(status().isUnprocessableEntity());
        register(card.getIin(), "Ахметова", "8 702 555 44 33", "new@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(card.getId()))
                .andExpect(jsonPath("$.username").value(card.getIin()));

        lookup(card.getIin(), "REGISTERED");
        Patient linked = patientRepository.findByIin(card.getIin()).orElseThrow();
        assertThat(linked.getUser()).isNotNull();
        assertThat(linked.getAddress()).isEqualTo("Астана, пр. Мангилик Ел, 5, кв. 12");

        register(card.getIin(), "Ахметова", "8 702 555 44 33", "new@example.com").andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + card.getIin() + "\",\"password\":\"Patient123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Registration requires a 12-digit IIN, email and address")
    void validatesRegistration() throws Exception {
        lookup(iin(), "NEW");
        mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .header("Accept-Language", "kk")
                        .content("""
                                {"password":"Patient123!","iin":"12345","firstName":"А","lastName":"Б",
                                 "birthDate":"1990-01-01","gender":"MALE","phone":"+77010000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.iin").value("ЖСН 12 саннан тұруы керек"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.addressProvided").exists());
    }

    @Test
    @DisplayName("A visit is closed only with a conclusion; the certificate appears after that")
    void completesVisitAndIssuesCertificate() throws Exception {
        String complete = "/api/appointments/" + visit.getId() + "/complete";
        String certificate = "/api/appointments/" + visit.getId() + "/certificate";
        mockMvc.perform(get(certificate).with(user(patientPrincipal))).andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post(complete).with(user(doctorPrincipal)).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post(complete).with(user(doctorPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CONCLUSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("Прошел"))
                .andExpect(jsonPath("$.conclusion").value("Состояние стабильное"));
        mockMvc.perform(get(certificate).with(user(strangerPrincipal))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"ru,Медицинская справка,Адрес проживания", "kk,Медициналық анықтама,Тұрғылықты мекенжайы",
            "en,Medical certificate,Home address"})
    @DisplayName("The certificate is a PDF in the interface language")
    void certificateIsLocalised(String lang, String title, String address) throws Exception {
        mockMvc.perform(post("/api/appointments/" + visit.getId() + "/complete").with(user(doctorPrincipal))
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CONCLUSION)).andExpect(status().isOk());
        byte[] pdf = mockMvc.perform(get("/api/appointments/" + visit.getId() + "/certificate")
                        .header("Accept-Language", lang).with(user(patientPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        String text = text(pdf);
        assertThat(text).contains(title, address, patient.getIin(), "Состояние стабильное", patient.getFullName());
    }

    @Test
    @DisplayName("A diagnostic referral reaches the department that performs the examination")
    void routesDiagnosticReferral() throws Exception {
        createReferral().andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromOffice").value("118"))
                .andExpect(jsonPath("$.office").value("210"))
                .andExpect(jsonPath("$.price").value(4500.0));
        mockMvc.perform(get("/api/referrals/incoming").with(user(cardiologistPrincipal)).header("Accept-Language", "en"))
                .andExpect(jsonPath("$[0].patientIin").value(patient.getIin()))
                .andExpect(jsonPath("$[0].reason").value("Исключить аритмию"))
                .andExpect(jsonPath("$[0].serviceName").value("ECG"))
                .andExpect(jsonPath("$[0].statusLabel").value("Pending"));
        mockMvc.perform(get("/api/referrals/outgoing").with(user(doctorPrincipal)))
                .andExpect(jsonPath("$[0].patientName").value(patient.getFullName()));
        mockMvc.perform(get("/api/appointments/" + visit.getId() + "/referrals").with(user(patientPrincipal)))
                .andExpect(jsonPath("$[0].serviceId").value(ecg.getId()));
    }

    @Test
    @DisplayName("Online payment marks services as paid and sends a receipt in the patient's language")
    void paysOnlineAndEmailsReceipt() throws Exception {
        long referralId = JsonPath.<Number>read(createReferral().andReturn().getResponse().getContentAsString(), "$.id").longValue();
        mockMvc.perform(get("/api/me/payables").with(user(patientPrincipal)))
                .andExpect(jsonPath("$.length()").value(2));
        String items = """
                [{"type":"APPOINTMENT","id":%d},{"type":"REFERRAL","id":%d}]""".formatted(visit.getId(), referralId);

        pay(items, "4000000000000002").andExpect(status().isUnprocessableEntity());
        pay(items, "4111111111111111").andExpect(status().isUnprocessableEntity());
        assertThat(paymentRepository.findByPatientIdOrderByPaidAtDesc(patient.getId())).isEmpty();

        String response = pay(items, "4242 4242 4242 4242").andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(13500.0))
                .andExpect(jsonPath("$.cardLast4").value("4242"))
                .andReturn().getResponse().getContentAsString();
        long paymentId = JsonPath.<Number>read(response, "$.id").longValue();
        String number = JsonPath.read(response, "$.number");

        pay(items, "4242424242424242").andExpect(status().isConflict());
        mockMvc.perform(get("/api/me/payables").with(user(patientPrincipal)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/me/visits").with(user(patientPrincipal)))
                .andExpect(jsonPath("$[0].paymentStatusLabel").value("Оплачено"));

        assertThat(awaitDelivery(paymentId).getEmailStatus()).isEqualTo(ReceiptEmailStatus.SAVED);
        try (Stream<Path> files = Files.list(Path.of("target/mail-outbox"))) {
            List<Path> saved = files.filter(file -> file.getFileName().toString().contains(number)).toList();
            assertThat(saved).hasSize(1);
            assertThat(Files.readString(saved.get(0))).contains("To: anna@example.com", "application/pdf");
        }

        byte[] pdf = mockMvc.perform(get("/api/me/payments/" + paymentId + "/receipt")
                        .header("Accept-Language", "kk").with(user(patientPrincipal)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(text(pdf)).contains(number, patient.getIin(), "Кеңес беру", "13 500 тг");
        mockMvc.perform(get("/api/me/payments/" + paymentId + "/receipt").with(user(strangerPrincipal)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("The cash desk records a payment; a patient cannot use it")
    void paysAtCashDesk() throws Exception {
        String request = """
                {"patientId":%d,"method":"CASH","items":[{"type":"APPOINTMENT","id":%d}]}"""
                .formatted(patient.getId(), visit.getId());
        mockMvc.perform(post("/api/payments/cash-desk").with(user(patientPrincipal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/payments/cash-desk").with(user(receptionPrincipal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.methodLabel").value("Касса, наличные"));
        mockMvc.perform(get("/api/payments").with(user(receptionPrincipal)))
                .andExpect(jsonPath("$[0].patientIin").value(patient.getIin()));
    }

    @Test
    @DisplayName("A patient cannot pay for somebody else's visit")
    void rejectsForeignVisit() throws Exception {
        Patient other = patientRepository.save(card(iin(), "Чужая", "+7 (701) 000-00-00", "x@example.com"));
        Appointment foreign = appointmentRepository.save(new Appointment(visit.getDoctor(), other, visit.getService(),
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2).plusMinutes(30),
                visit.getPrice(), userRepository.findById(receptionPrincipal.getId()).orElseThrow()));
        pay("[{\"type\":\"APPOINTMENT\",\"id\":" + foreign.getId() + "}]", "4242424242424242")
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions createReferral() throws Exception {
        return mockMvc.perform(post("/api/referrals").with(user(doctorPrincipal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"appointmentId\":%d,\"serviceId\":%d,\"reason\":\"Исключить аритмию\"}"
                        .formatted(visit.getId(), ecg.getId())));
    }

    private org.springframework.test.web.servlet.ResultActions pay(String items, String number) throws Exception {
        return mockMvc.perform(post("/api/me/payments").with(user(patientPrincipal)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":" + items + ",\"card\":" + CARD.formatted(number) + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String iin, String lastName, String phone,
                                                                        String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"password":"Patient123!","iin":"%s","firstName":"Айгерим","lastName":"%s",
                         "birthDate":"1990-04-12","gender":"FEMALE","phone":"%s","email":"%s",
                         "city":"Астана","street":"пр. Мангилик Ел","house":"5","apartment":"12"}
                        """.formatted(iin, lastName, phone, email)));
    }

    private void lookup(String iin, String expected) throws Exception {
        mockMvc.perform(post("/api/auth/iin").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"iin\":\"" + iin + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expected))
                .andExpect(jsonPath("$.length()").value(1));
    }

    private Payment awaitDelivery(long id) throws InterruptedException {
        Payment payment = paymentRepository.findById(id).orElseThrow();
        for (int i = 0; i < 300 && payment.getEmailStatus() == ReceiptEmailStatus.PENDING; i++) {
            Thread.sleep(100);
            payment = paymentRepository.findById(id).orElseThrow();
        }
        return payment;
    }

    private Patient card(String iin, String lastName, String phone, String email) {
        return new Patient(iin, "Айгерим", lastName, LocalDate.of(1990, 4, 12), Gender.FEMALE, phone, email,
                "Алматы, ул. Абая, 10");
    }

    private Doctor doctor(String username, Specialization specialization, String office) {
        User account = userRepository.save(new User(username, "x", "Врач " + username, Role.DOCTOR));
        return doctorRepository.save(new Doctor(account, specialization, "Врач", username,
                username + "@example.com", "+7 (727) 000-00-00", office));
    }

    private String iin() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
    }

    private String text(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        StringBuilder text = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page)).append('\n');
        }
        return text.toString().replaceAll("\\s+", " ");
    }
}
