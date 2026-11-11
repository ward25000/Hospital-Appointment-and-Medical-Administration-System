package com.ward.hospital.controller;

import com.ward.hospital.dto.service.MedicalServiceRequest;
import com.ward.hospital.dto.service.MedicalServiceResponse;
import com.ward.hospital.service.ServiceCatalogService;
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
@RequestMapping("/api/services")
public class MedicalServiceController {

    private final ServiceCatalogService serviceCatalog;

    public MedicalServiceController(ServiceCatalogService serviceCatalog) {
        this.serviceCatalog = serviceCatalog;
    }

    @GetMapping
    public List<MedicalServiceResponse> findAll() {
        return serviceCatalog.findAll();
    }

    @GetMapping("/{id}")
    public MedicalServiceResponse findById(@PathVariable Long id) {
        return serviceCatalog.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalServiceResponse> create(@Valid @RequestBody MedicalServiceRequest request) {
        MedicalServiceResponse created = serviceCatalog.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicalServiceResponse update(@PathVariable Long id,
                                         @Valid @RequestBody MedicalServiceRequest request) {
        return serviceCatalog.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceCatalog.delete(id);
        return ResponseEntity.noContent().build();
    }
}
