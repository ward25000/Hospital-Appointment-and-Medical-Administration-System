package com.ward.hospital.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats shared by emails and PDF documents. */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Formats() {
    }

    public static String date(LocalDate value) {
        return DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    public static String money(BigDecimal value) {
        return number(value) + " ₸";
    }

    /** The PDF font has no tenge sign, so documents spell the currency out. */
    public static String moneyForPdf(String lang, BigDecimal value) {
        return number(value) + ("en".equals(lang) ? " KZT" : " тг");
    }

    private static String number(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }
}
