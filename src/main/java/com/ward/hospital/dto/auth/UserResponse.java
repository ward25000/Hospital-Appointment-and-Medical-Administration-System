package com.ward.hospital.dto.auth;

import com.ward.hospital.domain.Role;
import com.ward.hospital.security.UserPrincipal;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        String roleLabel,
        Long doctorId
) {

    public static UserResponse from(UserPrincipal principal) {
        return new UserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getFullName(),
                principal.getRole(),
                principal.getRole().getLabel(),
                principal.getDoctorId()
        );
    }
}
