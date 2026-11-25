package com.ward.hospital.service;

import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.AppointmentStatus;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.PaymentItem;
import com.ward.hospital.domain.PaymentMethod;
import com.ward.hospital.domain.PaymentStatus;
import com.ward.hospital.domain.ReceiptEmailStatus;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.domain.ReferralStatus;
import com.ward.hospital.domain.User;
import com.ward.hospital.dto.payment.CashDeskPaymentRequest;
import com.ward.hospital.dto.payment.OnlinePaymentRequest;
import com.ward.hospital.dto.payment.PayableRef;
import com.ward.hospital.dto.payment.PayableResponse;
import com.ward.hospital.dto.payment.PayableType;
import com.ward.hospital.dto.payment.PaymentResponse;
import com.ward.hospital.exception.BusinessRuleException;
import com.ward.hospital.exception.ConflictException;
import com.ward.hospital.exception.ForbiddenOperationException;
import com.ward.hospital.exception.NotFoundException;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.payment.PaymentCompletedEvent;
import com.ward.hospital.payment.PaymentGateway;
import com.ward.hospital.pdf.ReceiptPdf;
import com.ward.hospital.repository.AppointmentRepository;
import com.ward.hospital.repository.PatientRepository;
import com.ward.hospital.repository.PaymentRepository;
import com.ward.hospital.repository.ReferralRepository;
import com.ward.hospital.repository.UserRepository;
import com.ward.hospital.security.UserPrincipal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReferralRepository referralRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PaymentGateway gateway;
    private final ReceiptPdf receiptPdf;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(PaymentRepository paymentRepository, AppointmentRepository appointmentRepository,
                          ReferralRepository referralRepository, PatientRepository patientRepository,
                          UserRepository userRepository, PaymentGateway gateway, ReceiptPdf receiptPdf,
                          ApplicationEventPublisher events) {
        this.paymentRepository = paymentRepository;
        this.appointmentRepository = appointmentRepository;
        this.referralRepository = referralRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.gateway = gateway;
        this.receiptPdf = receiptPdf;
        this.events = events;
    }

    public List<PayableResponse> findPayables(Long patientId) {
        List<PayableResponse> result = new ArrayList<>();
        appointmentRepository.search(LocalDateTime.now().minusYears(10), LocalDateTime.now().plusYears(10),
                        null, patientId, null, null).stream()
                .filter(this::isPayable)
                .map(PayableResponse::from)
                .forEach(result::add);
        referralRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(this::isPayable)
                .map(PayableResponse::from)
                .forEach(result::add);
        result.sort(Comparator.comparing(PayableResponse::date));
        return result;
    }

    public List<PaymentResponse> findByPatient(Long patientId) {
        return paymentRepository.findByPatientIdOrderByPaidAtDesc(patientId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    public List<PaymentResponse> findInPeriod(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(30) : from;
        if (end.isBefore(start)) {
            throw new BusinessRuleException("Дата окончания периода не может быть раньше даты начала");
        }
        return paymentRepository.findInPeriod(start.atStartOfDay(), end.plusDays(1).atStartOfDay()).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public PaymentResponse payOnline(OnlinePaymentRequest request, UserPrincipal actor) {
        Patient patient = lockPatient(actor.getPatientId());
        patient.setPreferredLang(I18n.lang());
        Payment payment = build(patient, request.items(), PaymentMethod.ONLINE_CARD, actor, I18n.lang());
        PaymentGateway.Charge charge = gateway.charge(payment.getNumber(), payment.getAmount(), request.card());
        payment.setGatewayReference(charge.reference());
        payment.setCardLast4(charge.cardLast4());
        return complete(payment);
    }

    @Transactional
    public PaymentResponse payAtCashDesk(CashDeskPaymentRequest request, UserPrincipal actor) {
        if (!request.method().isCashDesk()) {
            throw new BusinessRuleException("В кассе принимаются наличные или карта через терминал");
        }
        Patient patient = lockPatient(request.patientId());
        return complete(build(patient, request.items(), request.method(), actor, patient.getPreferredLang()));
    }

    public byte[] receipt(Long id, UserPrincipal actor) {
        return receiptPdf.render(getAccessible(id, actor), I18n.lang());
    }

    @Transactional
    public PaymentResponse resendReceipt(Long id, UserPrincipal actor) {
        Payment payment = getAccessible(id, actor);
        String email = payment.getPatient().getEmail();
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("В профиле пациента не указан email");
        }
        payment.setEmail(email);
        payment.setLang(actor.isPatient() ? I18n.lang() : payment.getPatient().getPreferredLang());
        payment.setEmailStatus(ReceiptEmailStatus.PENDING);
        events.publishEvent(new PaymentCompletedEvent(payment.getId()));
        return PaymentResponse.from(payment);
    }

    private Payment build(Patient patient, List<PayableRef> refs, PaymentMethod method,
                          UserPrincipal actor, String lang) {
        User creator = userRepository.findById(actor.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Payment payment = new Payment(nextNumber(), patient, method, creator, lang);
        for (PayableRef ref : new LinkedHashSet<>(refs)) {
            payment.addItem(ref.type() == PayableType.APPOINTMENT
                    ? PaymentItem.of(payableAppointment(ref.id(), patient))
                    : PaymentItem.of(payableReferral(ref.id(), patient)));
        }
        if (payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Нет суммы к оплате");
        }
        return payment;
    }

    private PaymentResponse complete(Payment payment) {
        LocalDateTime paidAt = payment.getPaidAt();
        for (PaymentItem item : payment.getItems()) {
            if (item.getAppointment() != null) {
                item.getAppointment().setPaymentStatus(PaymentStatus.PAID);
                item.getAppointment().setPaidAt(paidAt);
            } else {
                item.getReferral().setPaymentStatus(PaymentStatus.PAID);
                item.getReferral().setPaidAt(paidAt);
            }
        }
        if (payment.getEmail() == null || payment.getEmail().isBlank()) {
            payment.setEmailStatus(ReceiptEmailStatus.NO_EMAIL);
        }
        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Услуга уже оплачена");
        }
        events.publishEvent(new PaymentCompletedEvent(payment.getId()));
        return PaymentResponse.from(payment);
    }

    private Appointment payableAppointment(Long id, Patient patient) {
        Appointment appointment = appointmentRepository.findDetailedById(id)
                .filter(item -> Objects.equals(item.getPatient().getId(), patient.getId()))
                .orElseThrow(() -> new NotFoundException("Приём не найден"));
        requireUnpaid(appointment.getPaymentStatus());
        if (!isPayable(appointment)) {
            throw new BusinessRuleException("Этот приём нельзя оплатить");
        }
        return appointment;
    }

    private Referral payableReferral(Long id, Patient patient) {
        Referral referral = referralRepository.findDetailedById(id)
                .filter(item -> Objects.equals(item.getPatient().getId(), patient.getId()))
                .orElseThrow(() -> new NotFoundException("Направление не найдено"));
        requireUnpaid(referral.getPaymentStatus());
        if (!isPayable(referral)) {
            throw new BusinessRuleException("Это направление нельзя оплатить");
        }
        return referral;
    }

    private void requireUnpaid(PaymentStatus status) {
        if (status == PaymentStatus.PAID) {
            throw new ConflictException("Услуга уже оплачена");
        }
    }

    private boolean isPayable(Appointment appointment) {
        return appointment.getPaymentStatus() == PaymentStatus.UNPAID
                && (appointment.getStatus() == AppointmentStatus.SCHEDULED
                || appointment.getStatus() == AppointmentStatus.COMPLETED)
                && appointment.getPrice().signum() > 0;
    }

    private boolean isPayable(Referral referral) {
        return referral.getPaymentStatus() == PaymentStatus.UNPAID
                && referral.getStatus() != ReferralStatus.CANCELLED
                && referral.getService() != null
                && referral.getPrice() != null && referral.getPrice().signum() > 0;
    }

    /** Payments of one patient run one after another, so a double click never charges twice. */
    private Patient lockPatient(Long id) {
        return patientRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Пациент не найден"));
    }

    private Payment getAccessible(Long id, UserPrincipal actor) {
        Payment payment = paymentRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Платёж не найден"));
        if (actor.isPatient() && !Objects.equals(payment.getPatient().getId(), actor.getPatientId())) {
            throw new ForbiddenOperationException("Пациент может просматривать только свои платежи");
        }
        return payment;
    }

    private String nextNumber() {
        return "%s-%06d".formatted(LocalDate.now().format(NUMBER_DATE), random.nextInt(1_000_000));
    }
}
