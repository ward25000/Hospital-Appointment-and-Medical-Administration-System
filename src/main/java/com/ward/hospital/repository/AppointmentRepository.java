package com.ward.hospital.repository;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.dto.report.DoctorReportProjection;
import com.ward.hospital.dto.report.ServiceReportProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"doctor", "doctor.specialization", "patient", "service", "createdBy"})
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"doctor", "doctor.specialization", "patient", "service", "createdBy"})
    @Query("""
            select a from Appointment a
            where a.startTime >= :from and a.startTime < :to
              and (:doctorId is null or a.doctor.id = :doctorId)
              and (:patientId is null or a.patient.id = :patientId)
              and (:status is null or a.status = :status)
              and (:query is null
                   or lower(concat(a.patient.firstName, ' ', a.patient.lastName)) like lower(concat('%', :query, '%'))
                   or lower(concat(a.doctor.firstName, ' ', a.doctor.lastName)) like lower(concat('%', :query, '%'))
                   or a.patient.phone like concat('%', :query, '%'))
            order by a.startTime asc
            """)
    List<Appointment> search(@Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             @Param("doctorId") Long doctorId,
                             @Param("patientId") Long patientId,
                             @Param("status") AppointmentStatus status,
                             @Param("query") String query);

    @Query("""
            select count(a) from Appointment a
            where a.doctor.id = :doctorId
              and a.status <> :cancelled
              and a.startTime < :end
              and a.endTime > :start
              and (:excludeId is null or a.id <> :excludeId)
            """)
    long countOverlapping(@Param("doctorId") Long doctorId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("cancelled") AppointmentStatus cancelled,
                          @Param("excludeId") Long excludeId);

    @EntityGraph(attributePaths = {"doctor", "doctor.specialization", "patient", "service", "createdBy"})
    List<Appointment> findTop5ByStatusAndStartTimeAfterOrderByStartTimeAsc(AppointmentStatus status, LocalDateTime time);

    @EntityGraph(attributePaths = {"doctor", "doctor.specialization", "patient", "service", "createdBy"})
    List<Appointment> findTop5ByDoctorIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long doctorId, AppointmentStatus status, LocalDateTime time);

    List<Appointment> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime from, LocalDateTime to);

    long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    long countByDoctorIdAndStartTimeBetween(Long doctorId, LocalDateTime from, LocalDateTime to);

    long countByStatusAndStartTimeBetween(AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    long countByStatusAndDoctorIdAndStartTimeBetween(AppointmentStatus status, Long doctorId,
                                                     LocalDateTime from, LocalDateTime to);

    long countByPatientId(Long patientId);

    long countByServiceId(Long serviceId);

    long countByDoctorId(Long doctorId);

    @Query("""
            select count(distinct a.patient.id) from Appointment a
            where a.startTime >= :from and a.startTime < :to
              and (:doctorId is null or a.doctor.id = :doctorId)
            """)
    long countDistinctPatientsInPeriod(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("doctorId") Long doctorId);

    @Query("""
            select coalesce(sum(a.price), 0) from Appointment a
            where a.status = :status
              and a.startTime >= :from and a.startTime < :to
              and (:doctorId is null or a.doctor.id = :doctorId)
            """)
    BigDecimal sumRevenue(@Param("status") AppointmentStatus status,
                          @Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("doctorId") Long doctorId);

    @Query("""
            select a.doctor.id as doctorId,
                   concat(a.doctor.firstName, ' ', a.doctor.lastName) as doctorName,
                   a.doctor.specialization.name as specialization,
                   count(a) as total,
                   sum(case when a.status = :completed then 1 else 0 end) as completed,
                   sum(case when a.status = :cancelled then 1 else 0 end) as cancelled,
                   sum(case when a.status = :scheduled then 1 else 0 end) as scheduled,
                   coalesce(sum(case when a.status = :completed then a.price else 0 end), 0) as revenue
            from Appointment a
            where a.startTime >= :from and a.startTime < :to
            group by a.doctor.id, a.doctor.firstName, a.doctor.lastName, a.doctor.specialization.name
            order by count(a) desc, a.doctor.lastName asc
            """)
    List<DoctorReportProjection> reportByDoctor(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("completed") AppointmentStatus completed,
                                                @Param("cancelled") AppointmentStatus cancelled,
                                                @Param("scheduled") AppointmentStatus scheduled);

    @Query("""
            select a.service.id as serviceId,
                   a.service.name as serviceName,
                   count(a) as total,
                   sum(case when a.status = :completed then 1 else 0 end) as completed,
                   coalesce(sum(case when a.status = :completed then a.price else 0 end), 0) as revenue
            from Appointment a
            where a.startTime >= :from and a.startTime < :to
            group by a.service.id, a.service.name
            order by count(a) desc, a.service.name asc
            """)
    List<ServiceReportProjection> reportByService(@Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to,
                                                  @Param("completed") AppointmentStatus completed);
}
