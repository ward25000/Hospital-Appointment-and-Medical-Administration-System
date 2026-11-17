package com.ward.hospital.controller;

import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.patient.PatientResponse;
import com.ward.hospital.dto.patient.PatientSelfUpdateRequest;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.AppointmentService;
import com.ward.hospital.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@PreAuthorize("hasRole('PATIENT')")
public class PatientPortalController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;

    public PatientPortalController(PatientService patientService,
                                   AppointmentService appointmentService,
                                   CurrentUserService currentUserService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/profile")
    public PatientResponse profile() {
        return patientService.findMyProfile();
    }

    @PutMapping("/profile")
    public PatientResponse updateProfile(@Valid @RequestBody PatientSelfUpdateRequest request) {
        return patientService.updateMyProfile(request);
    }

    @GetMapping("/visits")
    public List<AppointmentResponse> visits() {
        return appointmentService.findMyVisits(currentUserService.requirePrincipal());
    }
}
