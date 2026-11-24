package com.ward.hospital.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ward.hospital.config.ClinicProperties;
import com.ward.hospital.i18n.I18n;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Fonts with Kazakh glyphs, the clinic letterhead and the round stamp. */
@Component
public class PdfKit {

    static final Color GREEN = new Color(0x21, 0x9b, 0x80);
    static final Color INK = new Color(0x20, 0x3c, 0x35);
    static final Color MUTED = new Color(0x5d, 0x70, 0x69);
    static final Color LINE = new Color(0xdf, 0xe8, 0xe4);
    private static final Color STAMP = new Color(0x2b, 0x4f, 0xa8);

    private final ClinicProperties clinic;
    private final BaseFont regular;
    private final BaseFont bold;

    public PdfKit(ClinicProperties clinic) {
        this.clinic = clinic;
        this.regular = load("LiberationSans-Regular.ttf");
        this.bold = load("LiberationSans-Bold.ttf");
    }

    interface Content {
        void write(Document document, PdfWriter writer) throws DocumentException;
    }

    byte[] build(String title, Content content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 56, 48, 44, 44);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        document.addTitle(title);
        document.open();
        content.write(document, writer);
        document.close();
        return output.toByteArray();
    }

    Font font(float size, Color color) {
        return new Font(regular, size, Font.NORMAL, color);
    }

    Font boldFont(float size, Color color) {
        return new Font(bold, size, Font.NORMAL, color);
    }

    void letterhead(Document document, String lang) {
        PdfPTable head = new PdfPTable(1);
        head.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(GREEN);
        cell.setBorderWidthBottom(1.4f);
        cell.setPaddingBottom(9);
        cell.addElement(new Paragraph(clinic.name(lang), boldFont(14, GREEN)));
        cell.addElement(new Paragraph(clinic.address(lang) + " · " + clinic.phone() + " · " + clinic.email(),
                font(9, MUTED)));
        cell.addElement(new Paragraph(I18n.translate(lang, "БИН") + " " + clinic.bin() + " · "
                + I18n.translate(lang, "Лицензия") + " " + clinic.license(), font(9, MUTED)));
        head.addCell(cell);
        document.add(head);
    }

    Paragraph title(String text) {
        Paragraph paragraph = new Paragraph(text, boldFont(15, INK));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(20);
        paragraph.setSpacingAfter(3);
        return paragraph;
    }

    Paragraph subtitle(String text) {
        Paragraph paragraph = new Paragraph(text, font(10, MUTED));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(16);
        return paragraph;
    }

    PdfPTable rows() {
        PdfPTable table = new PdfPTable(new float[]{32, 68});
        table.setWidthPercentage(100);
        return table;
    }

    void row(PdfPTable table, String label, String value) {
        table.addCell(cell(label, font(10, MUTED), Element.ALIGN_LEFT));
        table.addCell(cell(value == null || value.isBlank() ? "—" : value, font(10.5f, INK), Element.ALIGN_LEFT));
    }

    PdfPCell cell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(LINE);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(7);
        cell.setPaddingLeft(0);
        cell.setHorizontalAlignment(align);
        cell.setLeading(2, 1.15f);
        return cell;
    }

    /** Round stamp with the clinic name along the rim and the BIN in the middle. */
    void stamp(PdfWriter writer, float x, float y, String lang) {
        PdfContentByte canvas = writer.getDirectContent();
        canvas.saveState();
        canvas.setColorStroke(STAMP);
        canvas.setColorFill(STAMP);
        canvas.setLineWidth(1.6f);
        canvas.circle(x, y, 52);
        canvas.stroke();
        canvas.setLineWidth(0.7f);
        canvas.circle(x, y, 35);
        canvas.stroke();

        String rim = clinic.name(lang).toUpperCase() + " • ";
        float size = 7.2f;
        float total = bold.getWidthPoint(rim, size);
        float circumference = (float) (2 * Math.PI * 40);
        int repeat = Math.max(1, (int) (circumference / total));
        String text = rim.repeat(repeat);
        float stretch = circumference / (total * repeat);
        if (stretch < 1) {
            size *= stretch;
            stretch = 1;
        }
        double angle = Math.PI / 2;
        canvas.beginText();
        canvas.setFontAndSize(bold, size);
        for (char letter : text.toCharArray()) {
            double step = bold.getWidthPoint(String.valueOf(letter), size) * stretch / 40;
            double middle = angle - step / 2;
            canvas.showTextAligned(Element.ALIGN_CENTER, String.valueOf(letter),
                    (float) (x + 40 * Math.cos(middle)), (float) (y + 40 * Math.sin(middle)),
                    (float) Math.toDegrees(middle - Math.PI / 2));
            angle -= step;
        }
        canvas.setFontAndSize(regular, 7);
        canvas.showTextAligned(Element.ALIGN_CENTER, I18n.translate(lang, "БИН"), x, y + 5, 0);
        canvas.setFontAndSize(bold, 7.4f);
        canvas.showTextAligned(Element.ALIGN_CENTER, clinic.bin(), x, y - 6, 0);
        canvas.endText();
        canvas.restoreState();
    }

    ClinicProperties clinic() {
        return clinic;
    }

    private BaseFont load(String name) {
        try (InputStream input = PdfKit.class.getResourceAsStream("/liberation/" + name)) {
            if (input == null) {
                throw new IllegalStateException("Font " + name + " is missing");
            }
            return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, input.readAllBytes(), null);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load font " + name, ex);
        }
    }
}
