package com.ward.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referrals", indexes = {
        @Index(name = "idx_referrals_patient", columnList = "patient_id"),
        @Index(name = "idx_referrals_to_doctor", columnList = "to_doctor_id,status"),
        @Index(name = "idx_referrals_to_specialization", columnList = "to_specialization_id,status"),
        @Index(name = "idx_referrals_from_doctor", columnList = "from_doctor_id")
})
public class Referral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_doctor_id", nullable = false)
    private Doctor fromDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_doctor_id")
    private Doctor toDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_specialization_id")
    private Specialization toSpecialization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private MedicalService service;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 10)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "department", length = 120)
    private String department;

    @Column(name = "office", length = 30)
    private String office;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReferralStatus status = ReferralStatus.PENDING;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    protected Referral() {
    }

    public Referral(Patient patient, Doctor fromDoctor, Appointment appointment) {
        this.patient = patient;
        this.fromDoctor = fromDoctor;
        this.appointment = appointment;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getFromDoctor() {
        return fromDoctor;
    }

    public void setFromDoctor(Doctor fromDoctor) {
        this.fromDoctor = fromDoctor;
    }

    public Doctor getToDoctor() {
        return toDoctor;
    }

    public void setToDoctor(Doctor toDoctor) {
        this.toDoctor = toDoctor;
    }

    public Specialization getToSpecialization() {
        return toSpecialization;
    }

    public void setToSpecialization(Specialization toSpecialization) {
        this.toSpecialization = toSpecialization;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public MedicalService getService() {
        return service;
    }

    public void setService(MedicalService service) {
        this.service = service;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
