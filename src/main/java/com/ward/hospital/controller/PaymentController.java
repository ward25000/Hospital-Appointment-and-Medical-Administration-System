package com.ward.hospital.controller;

import com.ward.hospital.dto.payment.CashDeskPaymentRequest;
import com.ward.hospital.dto.payment.PayableResponse;
import com.ward.hospital.dto.payment.PaymentResponse;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Cash desk of the clinic. */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<PaymentResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return paymentService.findInPeriod(from, to);
    }

    @GetMapping("/payables")
    public List<PayableResponse> payables(@RequestParam Long patientId) {
        return paymentService.findPayables(patientId);
    }

    @PostMapping("/cash-desk")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody CashDeskPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.payAtCashDesk(request, currentUserService.requirePrincipal()));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        return PdfResponses.attachment("receipt-" + id + ".pdf",
                paymentService.receipt(id, currentUserService.requirePrincipal()));
    }

    @PostMapping("/{id}/resend")
    public PaymentResponse resend(@PathVariable Long id) {
        return paymentService.resendReceipt(id, currentUserService.requirePrincipal());
    }
}
