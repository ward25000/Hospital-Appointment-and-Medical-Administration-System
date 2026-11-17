package com.ward.hospital.repository;

import com.ward.hospital.domain.Referral;
import com.ward.hospital.domain.ReferralStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @EntityGraph(attributePaths = {
            "patient", "fromDoctor", "fromDoctor.specialization", "toDoctor",
            "toSpecialization", "appointment"
    })
    @Query("""
            select r from Referral r
            where (r.toDoctor.id = :doctorId
                   or (r.toDoctor is null and :specializationId is not null
                       and r.toSpecialization.id = :specializationId))
              and (:status is null or r.status = :status)
            order by r.createdAt desc
            """)
    List<Referral> searchIncoming(@Param("doctorId") Long doctorId,
                                  @Param("specializationId") Long specializationId,
                                  @Param("status") ReferralStatus status);

    @EntityGraph(attributePaths = {
            "patient", "fromDoctor", "fromDoctor.specialization", "toDoctor",
            "toSpecialization", "appointment"
    })
    List<Referral> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {
            "patient", "fromDoctor", "fromDoctor.specialization", "toDoctor",
            "toSpecialization", "appointment"
    })
    @Query("select r from Referral r where r.id = :id")
    java.util.Optional<Referral> findDetailedById(@Param("id") Long id);

    long countByToDoctorIdAndStatus(Long toDoctorId, ReferralStatus status);
}
