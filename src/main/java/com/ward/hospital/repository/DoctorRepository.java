package com.ward.hospital.repository;

import com.ward.hospital.domain.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
            select distinct d from Doctor d
            join fetch d.specialization
            join fetch d.user
            where (:specializationId is null or d.specialization.id = :specializationId)
              and (:query is null
                   or lower(concat(d.firstName, ' ', d.lastName)) like lower(concat('%', :query, '%'))
                   or lower(d.specialization.name) like lower(concat('%', :query, '%')))
            order by d.lastName, d.firstName
            """)
    List<Doctor> search(@Param("specializationId") Long specializationId, @Param("query") String query);

    @Query("select d from Doctor d join fetch d.specialization join fetch d.user where d.id = :id")
    Optional<Doctor> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"specialization", "user"})
    Optional<Doctor> findByUserId(Long userId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByUserUsernameIgnoreCase(String username);

    boolean existsByUserUsernameIgnoreCaseAndIdNot(String username, Long id);

    long countBySpecializationId(Long specializationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}
