package com.ward.hospital.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_number", columnNames = "number"),
        indexes = {
                @Index(name = "idx_payments_patient", columnList = "patient_id,paid_at"),
                @Index(name = "idx_payments_paid_at", columnList = "paid_at")
        })
public class Payment extends BaseEntity {

    @Column(name = "number", nullable = false, length = 30)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    /** Only the last four digits are ever stored; the full number never reaches the database. */
    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "gateway_reference", length = 60)
    private String gatewayReference;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "email", length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private ReceiptEmailStatus emailStatus = ReceiptEmailStatus.PENDING;

    @Column(name = "lang", nullable = false, length = 2)
    private String lang;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentItem> items = new ArrayList<>();

    protected Payment() {
    }

    public Payment(String number, Patient patient, PaymentMethod method, User createdBy, String lang) {
        this.number = number;
        this.patient = patient;
        this.method = method;
        this.createdBy = createdBy;
        this.lang = lang;
        this.paidAt = LocalDateTime.now();
        this.email = patient.getEmail();
        this.amount = BigDecimal.ZERO;
    }

    public void addItem(PaymentItem item) {
        item.setPayment(this);
        items.add(item);
        amount = amount.add(item.getAmount());
    }

    public String getNumber() {
        return number;
    }

    public Patient getPatient() {
        return patient;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public void setGatewayReference(String gatewayReference) {
        this.gatewayReference = gatewayReference;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ReceiptEmailStatus getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(ReceiptEmailStatus emailStatus) {
        this.emailStatus = emailStatus;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<PaymentItem> getItems() {
        return items;
    }
}
