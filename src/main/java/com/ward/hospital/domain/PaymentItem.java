package com.ward.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/** One paid service. Unique keys guarantee that a visit or a referral is never paid twice. */
@Entity
@Table(name = "payment_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_items_appointment", columnNames = "appointment_id"),
        @UniqueConstraint(name = "uk_payment_items_referral", columnNames = "referral_id")
})
public class PaymentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id")
    private Referral referral;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MedicalService service;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    protected PaymentItem() {
    }

    public static PaymentItem of(Appointment appointment) {
        PaymentItem item = new PaymentItem();
        item.appointment = appointment;
        item.service = appointment.getService();
        item.amount = appointment.getPrice();
        return item;
    }

    public static PaymentItem of(Referral referral) {
        PaymentItem item = new PaymentItem();
        item.referral = referral;
        item.service = referral.getService();
        item.amount = referral.getPrice();
        return item;
    }

    void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Payment getPayment() {
        return payment;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public Referral getReferral() {
        return referral;
    }

    public MedicalService getService() {
        return service;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
