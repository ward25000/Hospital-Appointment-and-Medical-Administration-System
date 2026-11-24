package com.ward.hospital.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Localisation of everything the server says: statuses, errors, emails and PDF documents.
 * Russian text is the key; the Kazakh and English versions live in
 * {@code static/i18n/dictionary.json}, which the browser loads as well.
 */
public final class I18n {

    public static final String RU = "ru";
    public static final String KK = "kk";
    public static final String EN = "en";
    public static final List<Locale> SUPPORTED = List.of(
            Locale.forLanguageTag(RU), Locale.forLanguageTag(KK), Locale.forLanguageTag(EN));

    private static final Map<String, List<String>> DICTIONARY = load();

    private static final Map<String, String[]> MONTHS = Map.of(
            RU, new String[]{"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа",
                    "сентября", "октября", "ноября", "декабря"},
            KK, new String[]{"қаңтар", "ақпан", "наурыз", "сәуір", "мамыр", "маусым", "шілде", "тамыз",
                    "қыркүйек", "қазан", "қараша", "желтоқсан"},
            EN, new String[]{"January", "February", "March", "April", "May", "June", "July", "August",
                    "September", "October", "November", "December"});

    private I18n() {
    }

    /** Language of the current request; Russian outside of a request. */
    public static String lang() {
        return LocaleContextHolder.getLocaleContext() == null
                ? RU : normalize(LocaleContextHolder.getLocale().getLanguage());
    }

    public static String normalize(String language) {
        if (language == null) {
            return RU;
        }
        String value = language.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith(KK) || value.startsWith("kz")) {
            return KK;
        }
        return value.startsWith(EN) ? EN : RU;
    }

    /** Translates into the language of the current request, replacing {0}, {1}… with arguments. */
    public static String t(String russian, Object... args) {
        return translate(lang(), russian, args);
    }

    public static String translate(String lang, String russian, Object... args) {
        if (russian == null) {
            return null;
        }
        String text = russian;
        List<String> translations = RU.equals(lang) ? null : DICTIONARY.get(russian);
        if (translations != null) {
            String candidate = translations.get(KK.equals(lang) ? 0 : 1);
            if (candidate != null && !candidate.isBlank()) {
                text = candidate;
            }
        }
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    /** Picks a localised value of a directory entry, falling back to Russian. */
    public static String pick(String lang, String ru, String kk, String en) {
        String value = KK.equals(lang) ? kk : EN.equals(lang) ? en : ru;
        return value == null || value.isBlank() ? ru : value;
    }

    public static String longDate(String lang, LocalDate date) {
        String month = MONTHS.get(lang)[date.getMonthValue() - 1];
        return switch (lang) {
            case KK -> date.getYear() + " ж. " + date.getDayOfMonth() + " " + month;
            case EN -> date.getDayOfMonth() + " " + month + " " + date.getYear();
            default -> date.getDayOfMonth() + " " + month + " " + date.getYear() + " г.";
        };
    }

    private static Map<String, List<String>> load() {
        try (InputStream input = I18n.class.getResourceAsStream("/static/i18n/dictionary.json")) {
            if (input == null) {
                return Map.of();
            }
            return new ObjectMapper().readValue(input, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read the translation dictionary", ex);
        }
    }
}
