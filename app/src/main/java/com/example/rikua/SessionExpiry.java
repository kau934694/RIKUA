package com.example.rikua;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SessionExpiry {

    private static final Pattern DATE_TIME = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})[Tt ](\\d{2}:\\d{2}:\\d{2})"
                    + "(?:\\.(\\d{1,9}))?([Zz]|[+-]\\d{2}:?\\d{2})?$");

    private SessionExpiry() {
    }

    static boolean isFuture(String expiresAt, long nowMillis) {
        Long expiryMillis = parseMillis(expiresAt);
        return expiryMillis != null && expiryMillis > nowMillis;
    }

    static Long parseMillis(String expiresAt) {
        if (expiresAt == null) {
            return null;
        }

        String value = expiresAt.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.matches("\\d{10}")) {
            try {
                return Long.parseLong(value) * 1000L;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value.matches("\\d{13}")) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Matcher matcher = DATE_TIME.matcher(value);
        if (matcher.matches()) {
            String fraction = matcher.group(3);
            String milliseconds = fraction == null
                    ? "000"
                    : (fraction + "000").substring(0, 3);
            String offset = matcher.group(4);
            boolean hasOffset = offset != null;

            if (hasOffset) {
                offset = offset.toUpperCase(Locale.US);
                if ("Z".equals(offset)) {
                    offset = "+0000";
                } else {
                    offset = offset.replace(":", "");
                    if (!isValidOffset(offset)) {
                        return null;
                    }
                }
            }

            String normalized = matcher.group(1) + "T" + matcher.group(2)
                    + "." + milliseconds + (hasOffset ? offset : "");
            return parseDate(normalized, "yyyy-MM-dd'T'HH:mm:ss.SSS"
                    + (hasOffset ? "Z" : ""), hasOffset);
        }

        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return parseDate(value, "yyyy-MM-dd", false);
        }
        return null;
    }

    private static boolean isValidOffset(String offset) {
        if (!offset.matches("[+-]\\d{4}")) {
            return false;
        }
        int hours = Integer.parseInt(offset.substring(1, 3));
        int minutes = Integer.parseInt(offset.substring(3, 5));
        return minutes < 60 && (hours < 18 || (hours == 18 && minutes == 0));
    }

    private static Long parseDate(String value, String pattern, boolean hasOffset) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));

        ParsePosition position = new ParsePosition(0);
        java.util.Date parsed = format.parse(value, position);
        if (parsed == null || position.getIndex() != value.length()) {
            return null;
        }
        return parsed.getTime();
    }
}
