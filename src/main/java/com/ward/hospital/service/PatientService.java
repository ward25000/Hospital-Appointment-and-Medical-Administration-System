package com.ward.hospital.service;

import com.ward.hospital.domain.Patient;
import com.ward.hospital.dto.patient.PatientRequest;
import com.ward.hospital.dto.patient.PatientResponse;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public PatientService(PatientRepository patientRepository, AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<PatientResponse> findAll(String query) {
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return patientRepository.search(normalized).stream()
                .map(PatientResponse::from)
                .toList();
    }

    public PatientResponse findById(Long id) {
        return PatientResponse.from(getEntity(id));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        Patient patient = new Patient(
                request.firstName().trim(),
                request.lastName().trim(),
                request.birthDate(),
                request.gender(),
                request.phone().trim(),
                normalize(request.email()),
                normalize(request.address())
        );
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = getEntity(id);
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhone(request.phone().trim());
        patient.setEmail(normalize(request.email()));
        patient.setAddress(normalize(request.address()));
        return PatientResponse.from(patient);
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = getEntity(id);
        long appointmentsCount = appointmentRepository.countByPatientId(id);
        if (appointmentsCount > 0) {
            throw new ConflictException("Нельзя удалить пациента: в системе есть записи на приём — "
                    + appointmentsCount);
        }
        patientRepository.delete(patient);
    }

    private Patient getEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
