package com.ward.hospital.payment;

import com.ward.hospital.config.ClinicProperties;
import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.PaymentItem;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.mail.OutgoingMail;
import com.ward.hospital.util.Formats;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ReceiptMailComposer {

    private final ClinicProperties clinic;

    public ReceiptMailComposer(ClinicProperties clinic) {
        this.clinic = clinic;
    }

    public OutgoingMail compose(Payment payment, byte[] pdf) {
        String lang = payment.getLang();
        StringBuilder rows = new StringBuilder();
        row(rows, t(lang, "Номер платежа"), payment.getNumber());
        row(rows, t(lang, "Дата и время"), Formats.dateTime(payment.getPaidAt()));
        row(rows, t(lang, "Пациент"), payment.getPatient().getFullName());
        row(rows, t(lang, "ИИН"), payment.getPatient().getIin());
        for (PaymentItem item : payment.getItems()) {
            row(rows, item.getService().name(lang), Formats.money(item.getAmount()));
        }
        row(rows, t(lang, "Итого оплачено"), Formats.money(payment.getAmount()));

        String html = """
                <div style="font-family:'Open Sans','Segoe UI',Arial,sans-serif;color:#203c35;max-width:560px">
                  <p style="font-size:18px;font-weight:600;color:#219b80;margin:0 0 12px">%s</p>
                  <p>%s</p>
                  <table style="border-collapse:collapse;width:100%%;font-size:14px">%s</table>
                  <p style="font-size:13px;color:#5d7069">%s</p>
                  <p style="font-size:12px;color:#5d7069;border-top:1px solid #dfe8e4;padding-top:10px">
                    %s<br>%s<br>%s · %s %s</p>
                </div>
                """.formatted(
                e(clinic.name(lang)),
                e(t(lang, "Здравствуйте! Оплата принята, спасибо.")),
                rows,
                e(t(lang, "Квитанция в формате PDF приложена к письму.")),
                e(clinic.name(lang)), e(clinic.address(lang)), e(clinic.phone()),
                e(t(lang, "БИН")), e(clinic.bin()));
        return new OutgoingMail(payment.getEmail(),
                t(lang, "Квитанция об оплате № {0}", payment.getNumber()),
                html, "receipt-" + payment.getNumber() + ".pdf", pdf);
    }

    private void row(StringBuilder rows, String label, String value) {
        rows.append("<tr><td style=\"padding:7px 0;border-bottom:1px solid #dfe8e4;color:#5d7069\">").append(e(label))
                .append("</td><td style=\"padding:7px 0;border-bottom:1px solid #dfe8e4;text-align:right\">")
                .append(e(value)).append("</td></tr>");
    }

    private String t(String lang, String text, Object... args) {
        return I18n.translate(lang, text, args);
    }

    private String e(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, "UTF-8");
    }
}
