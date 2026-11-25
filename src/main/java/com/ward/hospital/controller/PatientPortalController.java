package com.ward.hospital.controller;

import com.ward.hospital.dto.appointment.AppointmentResponse;
import com.ward.hospital.dto.patient.LanguageRequest;
import com.ward.hospital.dto.patient.PatientResponse;
import com.ward.hospital.dto.patient.PatientSelfUpdateRequest;
import com.ward.hospital.dto.payment.OnlinePaymentRequest;
import com.ward.hospital.dto.payment.PayableResponse;
import com.ward.hospital.dto.payment.PaymentResponse;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.AppointmentService;
import com.ward.hospital.service.PatientService;
import com.ward.hospital.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PatientPortalController(PatientService patientService,
                                   AppointmentService appointmentService,
                                   PaymentService paymentService,
                                   CurrentUserService currentUserService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.paymentService = paymentService;
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

    @PutMapping("/language")
    public ResponseEntity<Void> updateLanguage(@Valid @RequestBody LanguageRequest request) {
        patientService.updateMyLanguage(request.lang());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/visits")
    public List<AppointmentResponse> visits() {
        return appointmentService.findMyVisits(currentUserService.requirePrincipal());
    }

    @GetMapping("/payables")
    public List<PayableResponse> payables() {
        return paymentService.findPayables(currentUserService.requirePatientId());
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments() {
        return paymentService.findByPatient(currentUserService.requirePatientId());
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody OnlinePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.payOnline(request, currentUserService.requirePrincipal()));
    }

    @GetMapping("/payments/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        return PdfResponses.attachment("receipt-" + id + ".pdf",
                paymentService.receipt(id, currentUserService.requirePrincipal()));
    }

    @PostMapping("/payments/{id}/resend")
    public PaymentResponse resend(@PathVariable Long id) {
        return paymentService.resendReceipt(id, currentUserService.requirePrincipal());
    }
}
