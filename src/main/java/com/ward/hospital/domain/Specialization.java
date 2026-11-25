package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "specializations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_specializations_name", columnNames = "name")
})
public class Specialization extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "name_kk", length = 120)
    private String nameKk;

    @Column(name = "name_en", length = 120)
    private String nameEn;

    @Column(name = "description_kk", length = 500)
    private String descriptionKk;

    @Column(name = "description_en", length = 500)
    private String descriptionEn;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Specialization() {
    }

    public Specialization(String name, String description) {
        this.name = name;
        this.description = description;
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

    public Specialization translate(String kkName, String enName, String kkDescription, String enDescription) {
        nameKk = kkName;
        nameEn = enName;
        descriptionKk = kkDescription;
        descriptionEn = enDescription;
        return this;
    }
}
