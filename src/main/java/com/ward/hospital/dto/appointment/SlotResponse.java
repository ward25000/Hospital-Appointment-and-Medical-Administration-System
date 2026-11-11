package com.ward.hospital.dto.appointment;

import java.time.LocalDateTime;

public record SlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean available,
        String reason
) {
}
