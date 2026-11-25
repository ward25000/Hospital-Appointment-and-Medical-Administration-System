package com.ward.hospital.service;

import com.ward.hospital.i18n.I18n;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.dto.specialization.SpecializationRequest;
import com.ward.hospital.dto.specialization.SpecializationResponse;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.SpecializationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpecializationService {

    private final SpecializationRepository specializationRepository;
    private final DoctorRepository doctorRepository;

    public SpecializationService(SpecializationRepository specializationRepository,
                                 DoctorRepository doctorRepository) {
        this.specializationRepository = specializationRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<SpecializationResponse> findAll() {
        return specializationRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public SpecializationResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public SpecializationResponse create(SpecializationRequest request) {
        String name = request.name().trim();
        if (specializationRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Специализация с таким названием уже существует");
        }
        Specialization specialization = new Specialization(name, normalize(request.description()));
        applyTranslations(specialization, request);
        specialization.setActive(request.active() == null || request.active());
        return toResponse(specializationRepository.save(specialization));
    }

    @Transactional
    public SpecializationResponse update(Long id, SpecializationRequest request) {
        Specialization specialization = getEntity(id);
        String name = request.name().trim();
        if (specializationRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Специализация с таким названием уже существует");
        }
        specialization.setName(name);
        specialization.setDescription(normalize(request.description()));
        applyTranslations(specialization, request);
        if (request.active() != null) {
            specialization.setActive(request.active());
        }
        return toResponse(specialization);
    }

    @Transactional
    public void delete(Long id) {
        Specialization specialization = getEntity(id);
        long doctorsCount = doctorRepository.countBySpecializationId(id);
        if (doctorsCount > 0) {
            throw new ConflictException(I18n.t("Нельзя удалить специализацию: к ней привязано врачей — {0}", doctorsCount));
        }
        specializationRepository.delete(specialization);
    }

    private void applyTranslations(Specialization specialization, SpecializationRequest request) {
        specialization.translate(normalize(request.nameKk()), normalize(request.nameEn()),
                normalize(request.descriptionKk()), normalize(request.descriptionEn()));
    }

    private SpecializationResponse toResponse(Specialization specialization) {
        return SpecializationResponse.from(specialization,
                doctorRepository.countBySpecializationId(specialization.getId()));
    }

    private Specialization getEntity(Long id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Специализация не найдена"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
