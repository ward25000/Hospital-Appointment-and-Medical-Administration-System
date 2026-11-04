package com.ward.hospital.dto.report;

import java.math.BigDecimal;

public record ServiceReportResponse(
        Long serviceId,
        String serviceName,
        long total,
        long completed,
        BigDecimal revenue
) {
}
