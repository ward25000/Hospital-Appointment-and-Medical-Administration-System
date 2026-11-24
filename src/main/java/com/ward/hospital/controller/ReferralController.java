package com.ward.hospital.controller;

import com.ward.hospital.domain.ReferralStatus;
import com.ward.hospital.dto.referral.ReferralCreateRequest;
import com.ward.hospital.dto.referral.ReferralResponse;
import com.ward.hospital.dto.referral.ReferralStatusRequest;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.ReferralService;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;
    private final CurrentUserService currentUserService;

    public ReferralController(ReferralService referralService, CurrentUserService currentUserService) {
        this.referralService = referralService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<ReferralResponse> incoming(
            @RequestParam(required = false) ReferralStatus status) {
        return referralService.findIncoming(currentUserService.requirePrincipal(), status);
    }

    @GetMapping("/outgoing")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<ReferralResponse> outgoing() {
        return referralService.findOutgoing(currentUserService.requirePrincipal());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<ReferralResponse> mine() {
        return referralService.findForPatient(currentUserService.requirePrincipal());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ReferralResponse> create(@Valid @RequestBody ReferralCreateRequest request) {
        ReferralResponse created = referralService.create(request, currentUserService.requirePrincipal());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ReferralResponse updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody ReferralStatusRequest request) {
        return referralService.updateStatus(id, request, currentUserService.requirePrincipal());
    }
}
