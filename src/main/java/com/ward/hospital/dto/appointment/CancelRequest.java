package com.ward.hospital.dto.appointment;

import jakarta.validation.constraints.Size;

public record CancelRequest(
        @Size(max = 500, message = "Причина не должна превышать 500 символов")
        String reason
) {
}
