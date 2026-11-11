package com.ward.hospital.service;

import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.domain.User;
import com.ward.hospital.dto.doctor.DoctorCreateRequest;
import com.ward.hospital.dto.doctor.DoctorResponse;
import com.ward.hospital.dto.doctor.DoctorUpdateRequest;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         SpecializationRepository specializationRepository,
                         AppointmentRepository appointmentRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DoctorResponse> findAll(Long specializationId, String query) {
        String normalizedQuery = normalize(query);
        return doctorRepository.search(specializationId, normalizedQuery).stream()
                .map(DoctorResponse::from)
                .toList();
    }

    public DoctorResponse findById(Long id) {
        return DoctorResponse.from(getDetailed(id));
    }

    @Transactional
    public DoctorResponse create(DoctorCreateRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        if (doctorRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Врач с таким email уже существует");
        }
        Specialization specialization = getSpecialization(request.specializationId());
        if (!specialization.isActive()) {
            throw new BusinessRuleException("Выбранная специализация отключена");
        }

        User user = new User(username, passwordEncoder.encode(request.password()),
                fullName(request.firstName(), request.lastName()), Role.DOCTOR);
        userRepository.save(user);

        Doctor doctor = new Doctor(user, specialization,
                request.firstName().trim(), request.lastName().trim(),
                email, request.phone().trim(), request.office().trim());
        doctor.setBio(normalize(request.bio()));
        doctor.setActive(true);
        return DoctorResponse.from(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorResponse update(Long id, DoctorUpdateRequest request) {
        Doctor doctor = getDetailed(id);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (doctorRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Врач с таким email уже существует");
        }
        Specialization specialization = getSpecialization(request.specializationId());
        boolean sameSpecialization = doctor.getSpecialization().getId().equals(specialization.getId());
        if (!specialization.isActive() && !sameSpecialization) {
            throw new BusinessRuleException("Выбранная специализация отключена");
        }

        doctor.setFirstName(request.firstName().trim());
        doctor.setLastName(request.lastName().trim());
        doctor.setSpecialization(specialization);
        doctor.setEmail(email);
        doctor.setPhone(request.phone().trim());
        doctor.setOffice(request.office().trim());
        doctor.setBio(normalize(request.bio()));

        doctor.getUser().setFullName(fullName(request.firstName(), request.lastName()));
        if (request.active() != null) {
            doctor.setActive(request.active());
            doctor.getUser().setEnabled(request.active());
        }
        return DoctorResponse.from(doctor);
    }

    @Transactional
    public void delete(Long id) {
        Doctor doctor = getDetailed(id);
        long appointmentsCount = appointmentRepository.countByDoctorId(id);
        if (appointmentsCount > 0) {
            throw new ConflictException("Нельзя удалить врача: в системе есть приёмы — " + appointmentsCount
                    + ". Отключите профиль врача вместо удаления.");
        }
        User user = doctor.getUser();
        doctorRepository.delete(doctor);
        doctorRepository.flush();
        userRepository.delete(user);
    }

    private Doctor getDetailed(Long id) {
        return doctorRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Врач не найден"));
    }

    private Specialization getSpecialization(Long id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Специализация не найдена"));
    }

    private String fullName(String firstName, String lastName) {
        return firstName.trim() + " " + lastName.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
