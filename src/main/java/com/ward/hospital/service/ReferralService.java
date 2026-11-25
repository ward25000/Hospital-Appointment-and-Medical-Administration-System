package com.ward.hospital.service;

import com.ward.hospital.repository.MedicalServiceRepository;
import com.ward.hospital.domain.MedicalService;
import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.Doctor;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.domain.ReferralStatus;
import com.ward.hospital.domain.Specialization;
import com.ward.hospital.dto.referral.ReferralCreateRequest;
import com.ward.hospital.dto.referral.ReferralResponse;
import com.ward.hospital.dto.referral.ReferralStatusRequest;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ForbiddenOperationException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.DoctorRepository;
import com.ward.hospital.repository.ReferralRepository;
import com.ward.hospital.repository.SpecializationRepository;
import com.ward.hospital.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final MedicalServiceRepository medicalServiceRepository;

    public ReferralService(ReferralRepository referralRepository,
                           AppointmentRepository appointmentRepository,
                           DoctorRepository doctorRepository,
                           SpecializationRepository specializationRepository,
                           MedicalServiceRepository medicalServiceRepository) {
        this.referralRepository = referralRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.specializationRepository = specializationRepository;
        this.medicalServiceRepository = medicalServiceRepository;
    }

    public List<ReferralResponse> findIncoming(UserPrincipal actor, ReferralStatus status) {
        Doctor doctor = getDoctor(actor);
        Long specializationId = doctor.getSpecialization() == null
                ? null : doctor.getSpecialization().getId();
        return referralRepository.searchIncoming(doctor.getId(), specializationId, status).stream()
                .map(ReferralResponse::from)
                .toList();
    }

    public List<ReferralResponse> findOutgoing(UserPrincipal actor) {
        return referralRepository.findByFromDoctorIdOrderByCreatedAtDesc(getDoctor(actor).getId()).stream()
                .map(ReferralResponse::from)
                .toList();
    }

    public List<ReferralResponse> findForPatient(UserPrincipal actor) {
        return referralRepository.findByPatientIdOrderByCreatedAtDesc(actor.getPatientId()).stream()
                .map(ReferralResponse::from)
                .toList();
    }

    @Transactional
    public ReferralResponse create(ReferralCreateRequest request, UserPrincipal actor) {
        Doctor fromDoctor = getDoctor(actor);
        Appointment appointment = appointmentRepository.findDetailedById(request.appointmentId())
                .orElseThrow(() -> new NotFoundException("Приём не найден"));
        if (!Objects.equals(appointment.getDoctor().getId(), fromDoctor.getId())) {
            throw new ForbiddenOperationException("Направление можно выписать только во время своего приёма");
        }
        if (appointment.getStatus() != com.ward.hospital.domain.AppointmentStatus.SCHEDULED
                && appointment.getStatus() != com.ward.hospital.domain.AppointmentStatus.COMPLETED) {
            throw new BusinessRuleException("Направление можно выписать только для активного или завершённого приёма");
        }

        Doctor toDoctor = null;
        Specialization toSpecialization = null;
        if (request.toDoctorId() != null && request.toSpecializationId() != null) {
            throw new BusinessRuleException("Укажите либо конкретного врача, либо специальность");
        }
        MedicalService service = null;
        if (request.serviceId() != null) {
            service = medicalServiceRepository.findById(request.serviceId())
                    .filter(MedicalService::isActive)
                    .orElseThrow(() -> new NotFoundException("Услуга не найдена"));
        }
        if (request.toDoctorId() == null && request.toSpecializationId() == null
                && service != null && service.isDiagnostic()) {
            toSpecialization = service.getSpecialization();
        }
        if (request.toDoctorId() == null && request.toSpecializationId() == null && toSpecialization == null) {
            throw new BusinessRuleException("Укажите врача или специальность, к которому направляется пациент");
        }
        if (request.toDoctorId() != null) {
            toDoctor = doctorRepository.findById(request.toDoctorId())
                    .filter(Doctor::isActive)
                    .orElseThrow(() -> new NotFoundException("Врач-получатель не найден"));
            if (Objects.equals(toDoctor.getId(), fromDoctor.getId())) {
                throw new BusinessRuleException("Нельзя направить пациента к самому себе");
            }
        }
        if (request.toSpecializationId() != null) {
            toSpecialization = specializationRepository.findById(request.toSpecializationId())
                    .orElseThrow(() -> new NotFoundException("Специальность не найдена"));
        }

        String department = normalize(request.department());
        String office = normalize(request.office());
        if (department == null && office == null) {
            if (toDoctor != null) {
                office = toDoctor.getOffice();
                department = toDoctor.getSpecialization().getName();
            } else {
                department = toSpecialization.getName();
                office = service == null ? null : service.getOffice();
            }
        }

        Referral referral = new Referral(appointment.getPatient(), fromDoctor, appointment);
        referral.setToDoctor(toDoctor);
        referral.setToSpecialization(toSpecialization);
        if (service != null) {
            referral.setService(service);
            referral.setPrice(service.getPrice());
        }
        referral.setDepartment(department);
        referral.setOffice(office);
        referral.setReason(normalize(request.reason()));
        return ReferralResponse.from(referralRepository.save(referral));
    }

    @Transactional
    public ReferralResponse updateStatus(Long id, ReferralStatusRequest request, UserPrincipal actor) {
        Referral referral = referralRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Направление не найдено"));
        if (!actor.isAdmin()) {
            Doctor doctor = getDoctor(actor);
            boolean recipient = Objects.equals(referral.getToDoctor() == null ? null : referral.getToDoctor().getId(),
                    doctor.getId())
                    || (referral.getToDoctor() == null && referral.getToSpecialization() != null
                    && Objects.equals(referral.getToSpecialization().getId(), doctor.getSpecialization().getId()));
            if (!recipient) {
                throw new ForbiddenOperationException("Статус направления может менять только врач-получатель");
            }
        }
        if (!isValidTransition(referral.getStatus(), request.status())) {
            throw new BusinessRuleException("Недопустимый переход статуса направления");
        }
        referral.setStatus(request.status());
        referral.setDecidedAt(LocalDateTime.now());
        return ReferralResponse.from(referral);
    }

    private boolean isValidTransition(ReferralStatus from, ReferralStatus to) {
        return switch (from) {
            case PENDING -> to == ReferralStatus.ACCEPTED || to == ReferralStatus.CANCELLED;
            case ACCEPTED -> to == ReferralStatus.COMPLETED || to == ReferralStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    private Doctor getDoctor(UserPrincipal actor) {
        if (!actor.isDoctor() && !actor.isAdmin()) {
            throw new ForbiddenOperationException("Работа с направлениями доступна врачу");
        }
        if (actor.getDoctorId() == null) {
            throw new ForbiddenOperationException("Учётная запись не связана с врачом");
        }
        return doctorRepository.findById(actor.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
