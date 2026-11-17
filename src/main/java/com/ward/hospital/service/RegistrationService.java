package com.ward.hospital.service;

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

    @Transactional
    public UserPrincipal register(RegistrationRequest request) {
        String username = request.username().trim();
        String iin = request.iin().trim();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        if (patientRepository.existsByIin(iin)) {
            throw new ConflictException("Пациент с таким ИИН уже зарегистрирован");
        }

        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();
        User user = new User(username, passwordEncoder.encode(request.password()),
                firstName + " " + lastName, Role.PATIENT);
        userRepository.save(user);

        Patient patient = new Patient(iin, firstName, lastName, request.birthDate(), request.gender(),
                request.phone().trim(), normalize(request.email()), request.address().trim());
        patient.setUser(user);
        patientRepository.save(patient);

        return UserPrincipal.from(user, null, patient.getId());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
