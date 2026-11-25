package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "medical_services", uniqueConstraints = {
        @UniqueConstraint(name = "uk_medical_services_name", columnNames = "name")
})
public class MedicalService extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "name_kk", length = 150)
    private String nameKk;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @Column(name = "description_kk", length = 500)
    private String descriptionKk;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** Laboratory or instrumental examination performed in a diagnostic room. */
    @Column(name = "diagnostic", nullable = false)
    private boolean diagnostic;

    @Column(name = "office", length = 30)
    private String office;

    /** Department that performs the examination and receives referrals for it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected MedicalService() {
    }

    public MedicalService(String name, String description, BigDecimal price, int durationMinutes) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationMinutes = durationMinutes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNameKk() {
        return nameKk;
    }

    public void setNameKk(String nameKk) {
        this.nameKk = nameKk;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getDescriptionKk() {
        return descriptionKk;
    }

    public void setDescriptionKk(String descriptionKk) {
        this.descriptionKk = descriptionKk;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public void setDescriptionEn(String descriptionEn) {
        this.descriptionEn = descriptionEn;
    }

    public String name(String lang) {
        return I18n.pick(lang, name, nameKk, nameEn);
    }

    public String description(String lang) {
        return I18n.pick(lang, description, descriptionKk, descriptionEn);
    }

    public MedicalService translate(String kkName, String enName, String kkDescription, String enDescription) {
        nameKk = kkName;
        nameEn = enName;
        descriptionKk = kkDescription;
        descriptionEn = enDescription;
        return this;
    }

    public boolean isDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(boolean diagnostic) {
        this.diagnostic = diagnostic;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public MedicalService asDiagnostic(Specialization department, String room) {
        diagnostic = true;
        specialization = department;
        office = room;
        return this;
    }
}
