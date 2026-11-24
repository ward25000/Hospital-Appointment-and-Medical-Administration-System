package com.ward.hospital.pdf;

import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.ward.hospital.domain.Payment;
import com.ward.hospital.domain.PaymentItem;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.util.Formats;
import org.springframework.stereotype.Component;

@Component
public class ReceiptPdf {

    private final PdfKit kit;

    public ReceiptPdf(PdfKit kit) {
        this.kit = kit;
    }

    public byte[] render(Payment payment, String lang) {
        String title = t(lang, "Квитанция об оплате № {0}", payment.getNumber());
        return kit.build(title, (document, writer) -> {
            kit.letterhead(document, lang);
            document.add(kit.title(title));
            document.add(kit.subtitle(Formats.dateTime(payment.getPaidAt())));

            PdfPTable info = kit.rows();
            kit.row(info, t(lang, "Плательщик"), payment.getPatient().getFullName());
            kit.row(info, t(lang, "ИИН"), payment.getPatient().getIin());
            kit.row(info, t(lang, "Способ оплаты"), payment.getMethod().label(lang)
                    + (payment.getCardLast4() == null ? "" : " •••• " + payment.getCardLast4()));
            kit.row(info, t(lang, "Статус"), t(lang, "Оплачено"));
            document.add(info);

            PdfPTable items = new PdfPTable(new float[]{8, 66, 26});
            items.setWidthPercentage(100);
            items.setSpacingBefore(20);
            items.addCell(kit.cell("№", kit.font(9.5f, PdfKit.MUTED), Element.ALIGN_LEFT));
            items.addCell(kit.cell(t(lang, "Услуга"), kit.font(9.5f, PdfKit.MUTED), Element.ALIGN_LEFT));
            items.addCell(kit.cell(t(lang, "Сумма"), kit.font(9.5f, PdfKit.MUTED), Element.ALIGN_RIGHT));
            int index = 1;
            for (PaymentItem item : payment.getItems()) {
                items.addCell(kit.cell(String.valueOf(index++), kit.font(10.5f, PdfKit.INK), Element.ALIGN_LEFT));
                items.addCell(kit.cell(item.getService().name(lang), kit.font(10.5f, PdfKit.INK), Element.ALIGN_LEFT));
                items.addCell(kit.cell(Formats.moneyForPdf(lang, item.getAmount()), kit.font(10.5f, PdfKit.INK), Element.ALIGN_RIGHT));
            }
            items.addCell(kit.cell("", kit.font(10, PdfKit.INK), Element.ALIGN_LEFT));
            items.addCell(kit.cell(t(lang, "Итого оплачено"), kit.boldFont(11, PdfKit.INK), Element.ALIGN_LEFT));
            items.addCell(kit.cell(Formats.moneyForPdf(lang, payment.getAmount()), kit.boldFont(11, PdfKit.INK), Element.ALIGN_RIGHT));
            document.add(items);

            Paragraph requisites = new Paragraph(t(lang, "Реквизиты клиники"), kit.boldFont(10.5f, PdfKit.INK));
            requisites.setSpacingBefore(22);
            requisites.setSpacingAfter(5);
            document.add(requisites);
            PdfPTable bank = kit.rows();
            kit.row(bank, t(lang, "Получатель"), kit.clinic().name(lang));
            kit.row(bank, t(lang, "БИН"), kit.clinic().bin());
            kit.row(bank, t(lang, "ИИК"), kit.clinic().iik());
            kit.row(bank, t(lang, "Банк"), kit.clinic().bank(lang) + ", " + t(lang, "БИК") + " " + kit.clinic().bik());
            document.add(bank);

            kit.stamp(writer, 460, writer.getVerticalPosition(true) - 75, lang);
        });
    }

    private String t(String lang, String text, Object... args) {
        return I18n.translate(lang, text, args);
    }
}
