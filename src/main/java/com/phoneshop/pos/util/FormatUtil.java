package com.phoneshop.pos.util;

import com.phoneshop.pos.config.AppConfig;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormatUtil {
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public static String formatCurrency(double amount) {
        return AppConfig.getCurrencySymbol() + " " + CURRENCY_FORMAT.format(amount);
    }

    public static String formatAmountWithoutSymbol(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DATE_TIME_FORMATTER);
    }

    public static String formatDate(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(TIME_FORMATTER);
    }
}
