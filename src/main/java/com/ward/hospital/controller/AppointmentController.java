package com.ward.hospital.controller;

import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.dto.appointment.AppointmentCreateRequest;
import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.appointment.CancelRequest;
import com.ward.hospital.dto.appointment.RescheduleRequest;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;

    public AppointmentController(AppointmentService appointmentService,
                                 CurrentUserService currentUserService) {
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AppointmentResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String query) {
        return appointmentService.findAll(from, to, doctorId, patientId, status, query,
                currentUserService.requirePrincipal());
    }

    @GetMapping("/{id}")
    public AppointmentResponse findById(@PathVariable Long id) {
        return appointmentService.findById(id, currentUserService.requirePrincipal());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentCreateRequest request) {
        AppointmentResponse created = appointmentService.create(request,
                currentUserService.requirePrincipal());
        URI location = URI.create("/api/appointments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public AppointmentResponse reschedule(@PathVariable Long id,
                                          @Valid @RequestBody RescheduleRequest request) {
        return appointmentService.reschedule(id, request, currentUserService.requirePrincipal());
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public AppointmentResponse complete(@PathVariable Long id) {
        return appointmentService.complete(id, currentUserService.requirePrincipal());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public AppointmentResponse cancel(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) CancelRequest request) {
        CancelRequest effectiveRequest = request == null ? new CancelRequest(null) : request;
        return appointmentService.cancel(id, effectiveRequest, currentUserService.requirePrincipal());
    }
}
