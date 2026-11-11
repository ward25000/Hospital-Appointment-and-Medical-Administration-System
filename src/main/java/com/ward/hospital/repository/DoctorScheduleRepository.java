package com.ward.hospital.repository;

import com.ward.hospital.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    @EntityGraph(attributePaths = "doctor")
    List<DoctorSchedule> findByDoctorId(Long doctorId);

    Optional<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(Long doctorId, DayOfWeek dayOfWeek);

    boolean existsByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);

    boolean existsByDoctorIdAndDayOfWeekAndIdNot(Long doctorId, DayOfWeek dayOfWeek, Long id);
}
