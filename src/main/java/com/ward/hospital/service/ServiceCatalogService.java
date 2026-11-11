package com.ward.hospital.service;

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

    public ServiceCatalogService(MedicalServiceRepository serviceRepository,
                                 AppointmentRepository appointmentRepository) {
        this.serviceRepository = serviceRepository;
        this.appointmentRepository = appointmentRepository;
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
            throw new ConflictException("Нельзя удалить услугу: она используется в приёмах — " + appointmentsCount
                    + ". Отключите услугу вместо удаления.");
        }
        serviceRepository.delete(service);
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
