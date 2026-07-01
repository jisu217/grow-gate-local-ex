package com.growgate.common;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static String formatUtc(LocalDateTime value) {
        if (value == null) return null;
        return value.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
