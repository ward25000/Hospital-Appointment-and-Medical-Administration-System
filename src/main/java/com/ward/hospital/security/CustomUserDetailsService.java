package com.ward.hospital.security;

import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.User;
import com.ward.hospital.repository.DoctorRepository;
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

    public CustomUserDetailsService(UserRepository userRepository, DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        Long doctorId = null;
        if (user.getRole() == Role.DOCTOR) {
            doctorId = doctorRepository.findByUserId(user.getId())
                    .map(Doctor::getId)
                    .orElseThrow(() -> new UsernameNotFoundException("Профиль врача не найден"));
        }

        return UserPrincipal.from(user, doctorId);
    }
}
