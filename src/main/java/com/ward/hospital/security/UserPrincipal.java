package com.ward.hospital.security;

import com.ward.hospital.domain.Role;
import com.ward.hospital.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final String fullName;
    private final Role role;
    private final Long doctorId;
    private final boolean enabled;

    public UserPrincipal(Long id, String username, String password, String fullName,
                         Role role, Long doctorId, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.doctorId = doctorId;
        this.enabled = enabled;
    }

    public static UserPrincipal from(User user, Long doctorId) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getFullName(),
                user.getRole(),
                doctorId,
                user.isEnabled()
        );
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isDoctor() {
        return role == Role.DOCTOR;
    }

    public boolean isReceptionist() {
        return role == Role.RECEPTIONIST;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
