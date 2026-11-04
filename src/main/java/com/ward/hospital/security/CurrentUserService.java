package com.ward.hospital.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Требуется авторизация");
        }
        return principal;
    }

    public Long requireDoctorId() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getDoctorId() == null) {
            throw new AuthenticationCredentialsNotFoundException("Учётная запись не связана с врачом");
        }
        return principal.getDoctorId();
    }
}
