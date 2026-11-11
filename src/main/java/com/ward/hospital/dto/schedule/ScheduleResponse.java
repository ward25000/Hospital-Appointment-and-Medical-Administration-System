package com.ward.hospital.dto.schedule;

import com.ward.hospital.domain.DoctorSchedule;
import com.ward.hospital.util.DayOfWeekLabels;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResponse(
        Long id,
        Long doctorId,
        String doctorName,
        DayOfWeek dayOfWeek,
        String dayLabel,
        LocalTime startTime,
        LocalTime endTime,
        int slotMinutes,
        boolean active
) {

    public static ScheduleResponse from(DoctorSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getDoctor().getId(),
                schedule.getDoctor().getFullName(),
                schedule.getDayOfWeek(),
                DayOfWeekLabels.of(schedule.getDayOfWeek()),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getSlotMinutes(),
                schedule.isActive()
        );
    }
}
