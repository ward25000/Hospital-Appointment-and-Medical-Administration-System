package com.ward.hospital.service;

import com.ward.hospital.util.Address;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.dto.patient.PatientRequest;
import com.ward.hospital.dto.patient.PatientResponse;
import com.ward.hospital.dto.patient.PatientSelfUpdateRequest;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;

    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          CurrentUserService currentUserService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
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

    public PatientResponse findMyProfile() {
        return PatientResponse.from(getEntity(currentUserService.requirePatientId()));
    }

    @Transactional
    public PatientResponse create(PatientRequest request) {
        String iin = normalizeIin(request.iin());
        checkIinAvailable(iin, null);
        Patient patient = new Patient(
                iin,
                request.firstName().trim(),
                request.lastName().trim(),
                request.birthDate(),
                request.gender(),
                request.phone().trim(),
                normalize(request.email()),
                normalize(request.address())
        );
        patient.setMiddleName(normalize(request.middleName()));
        if (request.structuredAddress().isComplete()) {
            patient.applyAddress(request.structuredAddress());
        }
        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = getEntity(id);
        String iin = normalizeIin(request.iin());
        checkIinAvailable(iin, id);
        patient.setIin(iin);
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhone(request.phone().trim());
        patient.setEmail(normalize(request.email()));
        patient.setMiddleName(normalize(request.middleName()));
        updateAddress(patient, request.structuredAddress(), request.address());
        syncAccountName(patient);
        return PatientResponse.from(patient);
    }

    @Transactional
    public PatientResponse updateMyProfile(PatientSelfUpdateRequest request) {
        Patient patient = getEntity(currentUserService.requirePatientId());
        patient.setFirstName(request.firstName().trim());
        patient.setLastName(request.lastName().trim());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhone(request.phone().trim());
        patient.setEmail(normalize(request.email()));
        patient.setMiddleName(normalize(request.middleName()));
        updateAddress(patient, request.structuredAddress(), request.address());
        syncAccountName(patient);
        return PatientResponse.from(patient);
    }

    @Transactional
    public void updateMyLanguage(String lang) {
        getEntity(currentUserService.requirePatientId()).setPreferredLang(lang);
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = getEntity(id);
        long appointmentsCount = appointmentRepository.countByPatientId(id);
        if (appointmentsCount > 0) {
            throw new ConflictException(I18n.t("Нельзя удалить пациента: в системе есть записи на приём — {0}",
                    appointmentsCount));
        }
        patientRepository.delete(patient);
    }

    private void updateAddress(Patient patient, Address structured, String text) {
        if (structured.isComplete()) {
            patient.applyAddress(structured);
        } else if (!text.trim().equals(patient.getAddress())) {
            patient.setAddress(text.trim());
            patient.setAddressCity(null);
            patient.setAddressStreet(null);
            patient.setAddressHouse(null);
            patient.setAddressApartment(null);
        }
    }

    private void syncAccountName(Patient patient) {
        if (patient.getUser() != null) {
            patient.getUser().setFullName(patient.getFullName());
        }
    }

    private Patient getEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private void checkIinAvailable(String iin, Long currentId) {
        boolean exists = currentId == null
                ? patientRepository.existsByIin(iin)
                : patientRepository.existsByIinAndIdNot(iin, currentId);
        if (exists) {
            throw new ConflictException("Пациент с таким ИИН уже существует");
        }
    }

    private String normalizeIin(String iin) {
        return iin == null ? null : iin.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
