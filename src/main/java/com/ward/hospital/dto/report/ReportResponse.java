package com.ward.hospital.dto.report;

import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        LocalDate from,
        LocalDate to,
        ReportSummaryResponse summary,
        List<DoctorReportResponse> byDoctor,
        List<ServiceReportResponse> byService,
        List<DailyReportResponse> daily
) {
}
