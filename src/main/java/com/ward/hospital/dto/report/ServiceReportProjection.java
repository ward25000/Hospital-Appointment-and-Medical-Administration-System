package com.ward.hospital.dto.report;

import java.math.BigDecimal;

public interface ServiceReportProjection {

    Long getServiceId();

    String getServiceName();

    Long getTotal();

    Long getCompleted();

    BigDecimal getRevenue();
}
