package com.ward.hospital.dto.common;

import java.time.Instant;

public record HealthResponse(
        String status,
        String database,
        String application,
        String version,
        Instant timestamp
) {
}
