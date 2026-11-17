package com.ward.hospital.dto.referral;

import com.ward.hospital.domain.ReferralStatus;
import jakarta.validation.constraints.NotNull;

public record ReferralStatusRequest(
        @NotNull(message = "Укажите новый статус направления")
        ReferralStatus status
) {
}
