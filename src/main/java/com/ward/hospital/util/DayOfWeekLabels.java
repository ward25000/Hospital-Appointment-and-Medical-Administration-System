package com.ward.hospital.util;

import com.ward.hospital.i18n.I18n;

import java.time.DayOfWeek;

public final class DayOfWeekLabels {

    private DayOfWeekLabels() {
    }

    public static String of(DayOfWeek dayOfWeek) {
        return I18n.t(switch (dayOfWeek) {
            case MONDAY -> "Понедельник";
            case TUESDAY -> "Вторник";
            case WEDNESDAY -> "Среда";
            case THURSDAY -> "Четверг";
            case FRIDAY -> "Пятница";
            case SATURDAY -> "Суббота";
            case SUNDAY -> "Воскресенье";
        });
    }
}
