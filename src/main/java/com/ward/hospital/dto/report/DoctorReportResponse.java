package com.ward.hospital.dto.report;

import java.math.BigDecimal;

public record DoctorReportResponse(
        Long doctorId,
        String doctorName,
        String specialization,
        long total,
        long completed,
        long cancelled,
        long scheduled,
        BigDecimal revenue
) {
}
