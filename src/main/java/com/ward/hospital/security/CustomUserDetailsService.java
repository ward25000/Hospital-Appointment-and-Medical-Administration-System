package com.ward.hospital.security;

import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.User;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    DoctorRepository doctorRepository,
                                    PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = username.trim();
        User user = userRepository.findByUsername(login)
                .or(() -> patientRepository.findByIin(login).map(Patient::getUser))
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        Long doctorId = null;
        Long patientId = null;
        if (user.getRole() == Role.DOCTOR) {
            doctorId = doctorRepository.findByUserId(user.getId())
                    .map(Doctor::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Профиль врача не найден"));
        } else if (user.getRole() == Role.PATIENT) {
            patientId = patientRepository.findByUserId(user.getId())
                    .map(Patient::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Профиль пациента не найден"));
        }

        return UserPrincipal.from(user, doctorId, patientId);
    }
}
