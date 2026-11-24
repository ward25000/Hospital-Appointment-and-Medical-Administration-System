package com.ward.hospital.payment;

import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.ReceiptEmailStatus;
import com.ward.hospital.mail.MailService;
import com.ward.hospital.mail.OutgoingMail;
import com.ward.hospital.pdf.ReceiptPdf;
import com.ward.hospital.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/** Emails the receipt right after the payment is committed. A mail failure never cancels the payment. */
@Component
public class ReceiptDelivery {

    private static final Logger log = LoggerFactory.getLogger(ReceiptDelivery.class);

    private final PaymentRepository paymentRepository;
    private final ReceiptPdf receiptPdf;
    private final ReceiptMailComposer composer;
    private final MailService mailService;
    private final TransactionTemplate transaction;

    public ReceiptDelivery(PaymentRepository paymentRepository, ReceiptPdf receiptPdf,
                           ReceiptMailComposer composer, MailService mailService,
                           PlatformTransactionManager transactionManager) {
        this.paymentRepository = paymentRepository;
        this.receiptPdf = receiptPdf;
        this.composer = composer;
        this.mailService = mailService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        ReceiptEmailStatus status;
        try {
            OutgoingMail mail = transaction.execute(tx -> compose(event.paymentId()));
            status = mail == null ? ReceiptEmailStatus.NO_EMAIL
                    : mailService.send(mail) ? ReceiptEmailStatus.SENT : ReceiptEmailStatus.SAVED;
        } catch (Exception ex) {
            log.error("Receipt for payment {} was not delivered", event.paymentId(), ex);
            status = ReceiptEmailStatus.FAILED;
        }
        ReceiptEmailStatus result = status;
        transaction.executeWithoutResult(tx -> paymentRepository.findById(event.paymentId())
                .ifPresent(payment -> payment.setEmailStatus(result)));
    }

    private OutgoingMail compose(Long paymentId) {
        Payment payment = paymentRepository.findDetailedById(paymentId).orElseThrow();
        if (payment.getEmail() == null || payment.getEmail().isBlank()) {
            return null;
        }
        return composer.compose(payment, receiptPdf.render(payment, payment.getLang()));
    }
}
