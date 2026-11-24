package com.ward.hospital.domain;

import com.ward.hospital.i18n.I18n;
import com.ward.hospital.util.Address;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patients_iin", columnNames = "iin"),
        @UniqueConstraint(name = "uk_patients_user", columnNames = "user_id")
})
public class Patient extends BaseEntity {

    @Column(name = "iin", nullable = false, length = 12)
    private String iin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "middle_name", length = 80)
    private String middleName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "address_city", length = 100)
    private String addressCity;

    @Column(name = "address_street", length = 120)
    private String addressStreet;

    @Column(name = "address_house", length = 20)
    private String addressHouse;

    @Column(name = "address_apartment", length = 20)
    private String addressApartment;

    /** Language of notifications sent to the patient. */
    @Column(name = "preferred_lang", nullable = false, length = 2)
    private String preferredLang = I18n.RU;

    protected Patient() {
    }

    public Patient(String iin, String firstName, String lastName, LocalDate birthDate, Gender gender,
                   String phone, String email, String address) {
        this.iin = iin;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public String getIin() {
        return iin;
    }

    public void setIin(String iin) {
        this.iin = iin;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFullName() {
        return middleName == null ? lastName + " " + firstName
                : lastName + " " + firstName + " " + middleName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressHouse() {
        return addressHouse;
    }

    public void setAddressHouse(String addressHouse) {
        this.addressHouse = addressHouse;
    }

    public String getAddressApartment() {
        return addressApartment;
    }

    public void setAddressApartment(String addressApartment) {
        this.addressApartment = addressApartment;
    }

    public String getPreferredLang() {
        return preferredLang;
    }

    public void setPreferredLang(String preferredLang) {
        this.preferredLang = preferredLang;
    }

    public Address getStructuredAddress() {
        return Address.of(addressCity, addressStreet, addressHouse, addressApartment);
    }

    /** Stores the structured address together with its one-line Russian form. */
    public void applyAddress(Address value) {
        addressCity = value.city();
        addressStreet = value.street();
        addressHouse = value.house();
        addressApartment = value.apartment();
        address = value.format(I18n.RU);
    }

    public String addressText(String lang) {
        Address value = getStructuredAddress();
        return value.isComplete() ? value.format(lang) : address;
    }
}
