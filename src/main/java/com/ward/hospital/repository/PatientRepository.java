package com.ward.hospital.repository;

import com.ward.hospital.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("""
            select p from Patient p
            where :query is null
               or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :query, '%'))
               or p.phone like concat('%', :query, '%')
               or lower(coalesce(p.email, '')) like lower(concat('%', :query, '%'))
            order by p.lastName, p.firstName
            """)
    List<Patient> search(@Param("query") String query);
}
