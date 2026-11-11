package com.ward.hospital.dto.auth;

public record CsrfResponse(String headerName, String token) {
}
