package com.ward.hospital.repository;

import com.ward.hospital.domain.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {

    List<MedicalService> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
