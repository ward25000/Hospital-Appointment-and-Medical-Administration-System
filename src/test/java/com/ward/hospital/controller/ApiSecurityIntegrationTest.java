package com.ward.hospital.controller;

import com.ward.hospital.domain.Role;
import com.ward.hospital.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("HTTP authorization and CSRF rules")
class ApiSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final UserPrincipal admin = new UserPrincipal(
            1L, "admin", "unused", "Анна Администратор", Role.ADMIN, null, true);
    private final UserPrincipal doctor = new UserPrincipal(
            2L, "doctor", "unused", "Иван Врач", Role.DOCTOR, 1L, true);
    private final UserPrincipal receptionist = new UserPrincipal(
            3L, "reception", "unused", "Регистратор", Role.RECEPTIONIST, null, true);

    @Test
    @DisplayName("Anonymous API request is rejected with 401 JSON")
    void anonymousRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/doctors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Требуется авторизация"));
    }

    @Test
    @DisplayName("Doctor cannot open the patient registry")
    void doctorCannotOpenPatients() throws Exception {
        mockMvc.perform(get("/api/patients").with(user(doctor)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Doctor cannot open clinic reports")
    void doctorCannotOpenReports() throws Exception {
        mockMvc.perform(get("/api/reports").with(user(doctor)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Receptionist cannot create a doctor")
    void receptionistCannotCreateDoctor() throws Exception {
        mockMvc.perform(post("/api/doctors")
                        .with(user(receptionist))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"new-doctor","password":"Doctor123!","specializationId":1,
                                 "firstName":"Новый","lastName":"Врач","email":"new@example.com",
                                 "phone":"+7 (900) 000-00-00","office":"101"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can open reports")
    void adminCanOpenReports() throws Exception {
        mockMvc.perform(get("/api/reports?from=2026-01-01&to=2026-01-31").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    @DisplayName("Authenticated POST without a CSRF token is rejected")
    void missingCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"doctorId":1,"patientId":1,"serviceId":1,
                                 "startTime":"2030-01-01T10:00:00"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Health endpoint is public")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Doctor can read their own appointment list")
    void doctorCanReadOwnAppointments() throws Exception {
        mockMvc.perform(get("/api/appointments").with(user(doctor)))
                .andExpect(status().isOk());
    }
}
