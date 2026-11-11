package com.ward.hospital.controller;

import com.ward.hospital.dto.specialization.SpecializationRequest;
import com.ward.hospital.dto.specialization.SpecializationResponse;
import com.ward.hospital.service.SpecializationService;
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
@RequestMapping("/api/specializations")
public class SpecializationController {

    private final SpecializationService specializationService;

    public SpecializationController(SpecializationService specializationService) {
        this.specializationService = specializationService;
    }

    @GetMapping
    public List<SpecializationResponse> findAll() {
        return specializationService.findAll();
    }

    @GetMapping("/{id}")
    public SpecializationResponse findById(@PathVariable Long id) {
        return specializationService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpecializationResponse> create(@Valid @RequestBody SpecializationRequest request) {
        SpecializationResponse created = specializationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SpecializationResponse update(@PathVariable Long id,
                                         @Valid @RequestBody SpecializationRequest request) {
        return specializationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        specializationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
