package com.ward.hospital.controller;

import com.ward.hospital.dto.schedule.ScheduleRequest;
import com.ward.hospital.dto.schedule.ScheduleResponse;
import com.ward.hospital.exception.ForbiddenOperationException;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.security.UserPrincipal;
import com.ward.hospital.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CurrentUserService currentUserService;

    public ScheduleController(ScheduleService scheduleService, CurrentUserService currentUserService) {
        this.scheduleService = scheduleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/doctors/{doctorId}/schedules")
    public List<ScheduleResponse> findByDoctor(@PathVariable Long doctorId) {
        UserPrincipal principal = currentUserService.requirePrincipal();
        if (principal.isDoctor() && !doctorId.equals(principal.getDoctorId())) {
            throw new ForbiddenOperationException("Врач может просматривать только своё расписание");
        }
        return scheduleService.findByDoctor(doctorId);
    }

    @PostMapping("/doctors/{doctorId}/schedules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ScheduleResponse> create(@PathVariable Long doctorId,
                                                   @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse created = scheduleService.create(doctorId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/schedules/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return scheduleService.update(id, request);
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
