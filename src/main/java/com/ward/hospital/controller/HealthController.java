package com.ward.hospital.controller;

import com.ward.hospital.dto.common.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        boolean databaseUp;
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            databaseUp = result != null && result == 1;
        } catch (Exception ex) {
            databaseUp = false;
        }

        HealthResponse response = new HealthResponse(
                databaseUp ? "UP" : "DOWN",
                databaseUp ? "UP" : "DOWN",
                "hospital-appointment-system",
                "1.0.0",
                Instant.now()
        );
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
