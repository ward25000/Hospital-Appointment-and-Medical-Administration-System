package com.ward.hospital.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReportSummaryResponse(
        LocalDate from,
        LocalDate to,
        long totalAppointments,
        long scheduled,
        long completed,
        long cancelled,
        long uniquePatients,
        BigDecimal revenue,
        BigDecimal averageCheck
) {
}
