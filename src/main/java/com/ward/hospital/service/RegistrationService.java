package com.ward.hospital.service;

import com.ward.hospital.i18n.I18n;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.dto.auth.IinLookupResponse.Status;
import com.ward.hospital.dto.auth.IinLookupResponse;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.User;
import com.ward.hospital.dto.auth.RegistrationRequest;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               PatientRepository patientRepository,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Tells whether the clinic already keeps a card with this IIN. */
    @Transactional(readOnly = true)
    public IinLookupResponse lookup(String iin) {
        return new IinLookupResponse(patientRepository.findByIin(iin)
                .map(patient -> patient.getUser() == null ? Status.CARD_FOUND : Status.REGISTERED)
                .orElse(Status.NEW));
    }

    @Transactional
    public UserPrincipal register(RegistrationRequest request) {
        String iin = request.iin().trim();
        String username = normalize(request.username()) == null ? iin : request.username().trim();

        Patient card = patientRepository.findByIin(iin).orElse(null);
        if (card != null && card.getUser() != null) {
            throw new ConflictException("Пациент с таким ИИН уже зарегистрирован");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        if (card != null && !confirmsIdentity(card, request)) {
            throw new BusinessRuleException("Карта с этим ИИН есть в клинике, но фамилия и телефон или email "
                    + "с ней не совпадают. Обратитесь в регистратуру");
        }

        Patient patient = card != null ? card : new Patient(iin, request.firstName().trim(),
                request.lastName().trim(), request.birthDate(), request.gender(), request.phone().trim(),
                request.email().trim(), request.address());
        if (card == null || normalize(request.middleName()) != null) {
            patient.setMiddleName(normalize(request.middleName()));
        }
        patient.setPhone(request.phone().trim());
        patient.setEmail(request.email().trim());
        if (request.structuredAddress().isComplete()) {
            patient.applyAddress(request.structuredAddress());
        } else {
            patient.setAddress(request.address().trim());
        }
        patient.setPreferredLang(I18n.lang());

        User user = userRepository.save(new User(username, passwordEncoder.encode(request.password()),
                patient.getFullName(), Role.PATIENT));
        patient.setUser(user);
        patientRepository.save(patient);
        return UserPrincipal.from(user, null, patient.getId());
    }

    /**
     * An IIN alone is not a secret, so an existing card is handed over only when
     * the surname and at least one contact match what the clinic has on file.
     */
    private boolean confirmsIdentity(Patient card, RegistrationRequest request) {
        boolean sameSurname = card.getLastName().trim().equalsIgnoreCase(request.lastName().trim());
        boolean samePhone = digits(card.getPhone()).equals(digits(request.phone()));
        boolean sameEmail = card.getEmail() != null && card.getEmail().trim().equalsIgnoreCase(request.email().trim());
        return sameSurname && (samePhone || sameEmail);
    }

    /** Last ten digits: +7 and 8 prefixes mean the same number. */
    private String digits(String phone) {
        String value = phone.replaceAll("\\D", "");
        return value.length() > 10 ? value.substring(value.length() - 10) : value;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
