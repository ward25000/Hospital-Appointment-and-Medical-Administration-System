package com.ward.hospital.pdf;

import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.ward.hospital.domain.Appointment;
import com.ward.hospital.domain.Patient;
import com.ward.hospital.domain.Referral;
import com.ward.hospital.i18n.I18n;
import com.ward.hospital.util.Formats;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Medical certificate / extract for a completed visit. */
@Component
public class CertificatePdf {

    private static final float STAMP_SPACE = 150;

    private final PdfKit kit;

    public CertificatePdf(PdfKit kit) {
        this.kit = kit;
    }

    public static String number(Appointment appointment) {
        return "%d-%06d".formatted(appointment.getStartTime().getYear(), appointment.getId());
    }

    public byte[] render(Appointment visit, List<Referral> referrals, String lang) {
        String title = t(lang, "Медицинская справка № {0}", number(visit));
        Patient patient = visit.getPatient();
        return kit.build(title, (document, writer) -> {
            kit.letterhead(document, lang);
            document.add(kit.title(title));
            document.add(kit.subtitle(t(lang, "Выписка по результатам приёма врача")));

            PdfPTable table = kit.rows();
            kit.row(table, t(lang, "Пациент"), patient.getFullName());
            kit.row(table, t(lang, "ИИН"), patient.getIin());
            kit.row(table, t(lang, "Дата рождения"), Formats.date(patient.getBirthDate()));
            kit.row(table, t(lang, "Адрес проживания"), patient.addressText(lang));
            kit.row(table, t(lang, "Дата визита"), I18n.longDate(lang, visit.getStartTime().toLocalDate())
                    + ", " + Formats.dateTime(visit.getStartTime()).substring(11));
            kit.row(table, t(lang, "Врач"), visit.getDoctor().getFullName() + ", "
                    + visit.getDoctor().getSpecialization().name(lang)
                    + ", " + t(lang, "каб. {0}", visit.getDoctor().getOffice()));
            kit.row(table, t(lang, "Услуга"), visit.getService().name(lang));
            kit.row(table, t(lang, "Диагноз"), visit.getDiagnosis());
            kit.row(table, t(lang, "Заключение врача"), visit.getConclusion());
            if (visit.getRecommendations() != null) {
                kit.row(table, t(lang, "Рекомендации"), visit.getRecommendations());
            }
            if (!referrals.isEmpty()) {
                kit.row(table, t(lang, "Выданные направления"), referrals.stream()
                        .map(referral -> "• " + target(referral, lang) + ": " + referral.getReason())
                        .collect(Collectors.joining("\n")));
            }
            document.add(table);

            if (writer.getVerticalPosition(true) < STAMP_SPACE + document.bottom()) {
                document.newPage();
            }
            float y = writer.getVerticalPosition(true);
            Paragraph signature = new Paragraph(t(lang, "Врач") + ": " + visit.getDoctor().getFullName()
                    + "   ____________________", kit.font(10.5f, PdfKit.INK));
            signature.setSpacingBefore(44);
            document.add(signature);
            Paragraph issued = new Paragraph(t(lang, "Дата выдачи") + ": " + Formats.date(LocalDate.now()),
                    kit.font(9.5f, PdfKit.MUTED));
            issued.setSpacingBefore(6);
            document.add(issued);
            kit.stamp(writer, 450, y - 78, lang);
        });
    }

    private String target(Referral referral, String lang) {
        StringBuilder text = new StringBuilder();
        if (referral.getService() != null) {
            text.append(referral.getService().name(lang)).append(", ");
        }
        if (referral.getToDoctor() != null) {
            text.append(referral.getToDoctor().getFullName());
        } else if (referral.getToSpecialization() != null) {
            text.append(referral.getToSpecialization().name(lang));
        }
        if (referral.getOffice() != null) {
            text.append(", ").append(t(lang, "каб. {0}", referral.getOffice()));
        }
        return text.toString();
    }

    private String t(String lang, String text, Object... args) {
        return I18n.translate(lang, text, args);
    }
}
