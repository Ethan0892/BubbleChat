package dev.bubblecraft.bubblechat.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdwy])");

    private DurationParser() {
    }

    public static long parseToMillis(String input) {
        if (input == null || input.isBlank()) return -1;
        String s = input.trim().toLowerCase(Locale.ROOT);

        // allow plain number = seconds
        if (s.matches("\\d+")) {
            try {
                return Long.parseLong(s) * 1000L;
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        long total = 0L;
        Matcher m = TOKEN.matcher(s);
        int matches = 0;
        while (m.find()) {
            matches++;
            long value = Long.parseLong(m.group(1));
            char unit = m.group(2).charAt(0);
            total += switch (unit) {
                case 's' -> value * 1000L;
                case 'm' -> value * 60_000L;
                case 'h' -> value * 3_600_000L;
                case 'd' -> value * 86_400_000L;
                case 'w' -> value * 604_800_000L;
                case 'y' -> value * 31_536_000_000L;
                default -> 0L;
            };
        }

        return matches == 0 ? -1 : total;
    }
}
