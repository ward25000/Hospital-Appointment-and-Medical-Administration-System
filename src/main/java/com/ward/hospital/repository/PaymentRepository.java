package com.ward.hospital.repository;

import com.ward.hospital.domain.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"patient", "items", "items.service"})
    List<Payment> findByPatientIdOrderByPaidAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"patient", "items", "items.service"})
    @Query("""
            select p from Payment p
            where p.paidAt >= :from and p.paidAt < :to
            order by p.paidAt desc
            """)
    List<Payment> findInPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = {"patient", "items", "items.service"})
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findDetailedById(@Param("id") Long id);
}
