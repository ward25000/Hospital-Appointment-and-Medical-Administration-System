package com.ward.hospital.service;

import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.dto.service.MedicalServiceRequest;
import com.ward.hospital.dto.service.MedicalServiceResponse;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.MedicalServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final MedicalServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecializationRepository specializationRepository;

    public ServiceCatalogService(MedicalServiceRepository serviceRepository,
                                 AppointmentRepository appointmentRepository,
                                 SpecializationRepository specializationRepository) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
        this.specializationRepository = specializationRepository;
    }

    public List<MedicalServiceResponse> findAll() {
        return serviceRepository.findAllByOrderByNameAsc().stream()
                .map(MedicalServiceResponse::from)
                .toList();
    }

    public MedicalServiceResponse findById(Long id) {
        return MedicalServiceResponse.from(getEntity(id));
    }

    @Transactional
    public MedicalServiceResponse create(MedicalServiceRequest request) {
        String name = request.name().trim();
        if (serviceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Услуга с таким названием уже существует");
        }
        MedicalService service = new MedicalService(
                name,
                normalize(request.description()),
                normalizePrice(request.price()),
                request.durationMinutes()
        );
        applyDetails(service, request);
        service.setActive(request.active() == null || request.active());
        return MedicalServiceResponse.from(serviceRepository.save(service));
    }

    @Transactional
    public MedicalServiceResponse update(Long id, MedicalServiceRequest request) {
        MedicalService service = getEntity(id);
        String name = request.name().trim();
        if (serviceRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Услуга с таким названием уже существует");
        }
        service.setName(name);
        service.setDescription(normalize(request.description()));
        service.setPrice(normalizePrice(request.price()));
        service.setDurationMinutes(request.durationMinutes());
        applyDetails(service, request);
        if (request.active() != null) {
            service.setActive(request.active());
        }
        return MedicalServiceResponse.from(service);
    }

    @Transactional
    public void delete(Long id) {
        MedicalService service = getEntity(id);
        long appointmentsCount = appointmentRepository.countByServiceId(id);
        if (appointmentsCount > 0) {
            throw new ConflictException(I18n.t(
                    "Нельзя удалить услугу: она используется в приёмах — {0}. Отключите услугу вместо удаления.",
                    appointmentsCount));
        }
        serviceRepository.delete(service);
    }

    private void applyDetails(MedicalService service, MedicalServiceRequest request) {
        service.translate(normalize(request.nameKk()), normalize(request.nameEn()),
                normalize(request.descriptionKk()), normalize(request.descriptionEn()));
        boolean diagnostic = Boolean.TRUE.equals(request.diagnostic());
        if (diagnostic && request.specializationId() == null) {
            throw new BusinessRuleException("Для диагностической услуги укажите отделение");
        }
        service.setDiagnostic(diagnostic);
        service.setOffice(normalize(request.office()));
        service.setSpecialization(request.specializationId() == null ? null
                : specializationRepository.findById(request.specializationId())
                .orElseThrow(() -> new NotFoundException("Специализация не найдена")));
    }

    private MedicalService getEntity(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Услуга не найдена"));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
