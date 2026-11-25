package com.ward.hospital.controller;

import com.ward.hospital.dto.auth.IinLookupResponse;
import com.ward.hospital.dto.auth.IinLookupRequest;
import com.ward.hospital.dto.auth.CsrfResponse;
import com.ward.hospital.dto.auth.LoginRequest;
import com.ward.hospital.dto.auth.RegistrationRequest;
import com.ward.hospital.dto.auth.UserResponse;
import com.ward.hospital.security.CurrentUserService;
import com.ward.hospital.security.UserPrincipal;
import com.ward.hospital.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUserService currentUserService;
    private final RegistrationService registrationService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CurrentUserService currentUserService,
                          RegistrationService registrationService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.currentUserService = currentUserService;
        this.registrationService = registrationService;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        saveAuthentication(authentication, httpRequest, httpResponse);
        return ResponseEntity.ok(UserResponse.from((UserPrincipal) authentication.getPrincipal()));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        UserPrincipal principal = registrationService.register(request);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        saveAuthentication(authentication, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(principal));
    }

    @PostMapping("/iin")
    public IinLookupResponse lookupIin(@Valid @RequestBody IinLookupRequest request) {
        return registrationService.lookup(request.iin());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUserService.requirePrincipal());
    }

    private void saveAuthentication(Authentication authentication,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse httpResponse) {
        httpRequest.getSession(true);
        httpRequest.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
