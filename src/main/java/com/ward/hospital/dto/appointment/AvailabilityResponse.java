package com.ward.hospital.dto.appointment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        Long doctorId,
        String doctorName,
        LocalDate date,
        DayOfWeek dayOfWeek,
        String dayLabel,
        boolean workingDay,
        String message,
        int slotMinutes,
        int serviceDurationMinutes,
        List<SlotResponse> slots
) {
}
