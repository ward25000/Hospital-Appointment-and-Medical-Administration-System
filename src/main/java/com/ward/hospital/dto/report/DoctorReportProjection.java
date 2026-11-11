package com.ward.hospital.dto.report;

import java.math.BigDecimal;

public interface DoctorReportProjection {

    Long getDoctorId();

    String getDoctorName();

    String getSpecialization();

    Long getTotal();

    Long getCompleted();

    Long getCancelled();

    Long getScheduled();

    BigDecimal getRevenue();
}
