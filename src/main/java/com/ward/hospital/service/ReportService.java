package com.ward.hospital.service;

import java.util.stream.Collectors;
import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.dto.report.DailyReportResponse;
import com.ward.hospital.dto.report.DoctorReportProjection;
import com.ward.hospital.dto.report.DoctorReportResponse;
import com.ward.hospital.dto.report.ReportResponse;
import com.ward.hospital.dto.report.ReportSummaryResponse;
import com.ward.hospital.dto.report.ServiceReportProjection;
import com.ward.hospital.dto.report.ServiceReportResponse;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final int MAX_RANGE_DAYS = 366;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalServiceRepository medicalServiceRepository;

    public ReportService(AppointmentRepository appointmentRepository,
                         DoctorRepository doctorRepository,
                         MedicalServiceRepository medicalServiceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.medicalServiceRepository = medicalServiceRepository;
    }

    public ReportResponse buildReport(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.withDayOfMonth(1);

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessRuleException("Дата окончания периода не может быть раньше даты начала");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > MAX_RANGE_DAYS) {
            throw new BusinessRuleException(I18n.t("Период отчёта не должен превышать {0} дней", MAX_RANGE_DAYS));
        }

        LocalDateTime fromTime = effectiveFrom.atStartOfDay();
        LocalDateTime toTime = effectiveTo.plusDays(1).atStartOfDay();

        long total = appointmentRepository.countByStartTimeBetween(fromTime, toTime);
        long scheduled = appointmentRepository.countByStatusAndStartTimeBetween(
                AppointmentStatus.SCHEDULED, fromTime, toTime);
        long completed = appointmentRepository.countByStatusAndStartTimeBetween(
                AppointmentStatus.COMPLETED, fromTime, toTime);
        long cancelled = appointmentRepository.countByStatusAndStartTimeBetween(
                AppointmentStatus.CANCELLED, fromTime, toTime);
        long uniquePatients = appointmentRepository.countDistinctPatientsInPeriod(fromTime, toTime, null);
        BigDecimal revenue = appointmentRepository.sumRevenue(
                AppointmentStatus.COMPLETED, fromTime, toTime, null);

        BigDecimal averageCheck = completed == 0
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP);

        ReportSummaryResponse summary = new ReportSummaryResponse(
                effectiveFrom, effectiveTo, total, scheduled, completed, cancelled,
                uniquePatients, revenue, averageCheck);

        String lang = I18n.lang();
        Map<Long, String> specializations = doctorRepository.search(null, null).stream()
                .collect(Collectors.toMap(Doctor::getId, doctor -> doctor.getSpecialization().name(lang)));
        Map<Long, String> services = medicalServiceRepository.findAll().stream()
                .collect(Collectors.toMap(MedicalService::getId, service -> service.name(lang)));

        List<DoctorReportResponse> byDoctor = appointmentRepository
                .reportByDoctor(fromTime, toTime, AppointmentStatus.COMPLETED,
                        AppointmentStatus.CANCELLED, AppointmentStatus.SCHEDULED)
                .stream()
                .map(item -> mapDoctor(item, specializations))
                .toList();

        List<ServiceReportResponse> byService = appointmentRepository
                .reportByService(fromTime, toTime, AppointmentStatus.COMPLETED)
                .stream()
                .map(item -> mapService(item, services))
                .toList();

        List<DailyReportResponse> daily = buildDaily(fromTime, toTime, effectiveFrom, effectiveTo);

        return new ReportResponse(effectiveFrom, effectiveTo, summary, byDoctor, byService, daily);
    }

    private List<DailyReportResponse> buildDaily(LocalDateTime fromTime, LocalDateTime toTime,
                                                 LocalDate from, LocalDate to) {
        TreeMap<LocalDate, DailyAccumulator> byDate = new TreeMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            byDate.put(date, new DailyAccumulator());
        }
        for (Appointment appointment : appointmentRepository
                .findByStartTimeBetweenOrderByStartTimeAsc(fromTime, toTime)) {
            DailyAccumulator accumulator = byDate.computeIfAbsent(
                    appointment.getStartTime().toLocalDate(), key -> new DailyAccumulator());
            accumulator.add(appointment);
        }
        return byDate.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .toList();
    }

    private DoctorReportResponse mapDoctor(DoctorReportProjection projection, Map<Long, String> specializations) {
        return new DoctorReportResponse(
                projection.getDoctorId(),
                projection.getDoctorName(),
                specializations.getOrDefault(projection.getDoctorId(), projection.getSpecialization()),
                longValue(projection.getTotal()),
                longValue(projection.getCompleted()),
                longValue(projection.getCancelled()),
                longValue(projection.getScheduled()),
                projection.getRevenue() == null ? BigDecimal.ZERO : projection.getRevenue()
        );
    }

    private ServiceReportResponse mapService(ServiceReportProjection projection, Map<Long, String> services) {
        return new ServiceReportResponse(
                projection.getServiceId(),
                services.getOrDefault(projection.getServiceId(), projection.getServiceName()),
                longValue(projection.getTotal()),
                longValue(projection.getCompleted()),
                projection.getRevenue() == null ? BigDecimal.ZERO : projection.getRevenue()
        );
    }

    private long longValue(Long value) {
        return value == null ? 0 : value;
    }

    private static final class DailyAccumulator {
        private long total;
        private long scheduled;
        private long completed;
        private long cancelled;
        private BigDecimal revenue = BigDecimal.ZERO;

        void add(Appointment appointment) {
            total++;
            switch (appointment.getStatus()) {
                case SCHEDULED -> scheduled++;
                case COMPLETED -> {
                    completed++;
                    revenue = revenue.add(appointment.getPrice());
                }
                case CANCELLED -> cancelled++;
            }
        }

        DailyReportResponse toResponse(LocalDate date) {
            return new DailyReportResponse(date, total, scheduled, completed, cancelled, revenue);
        }
    }
}
