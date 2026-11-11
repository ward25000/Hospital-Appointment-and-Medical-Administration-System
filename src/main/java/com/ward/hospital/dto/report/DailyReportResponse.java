package com.ward.hospital.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyReportResponse(
        LocalDate date,
        long total,
        long scheduled,
        long completed,
        long cancelled,
        BigDecimal revenue
) {
}
