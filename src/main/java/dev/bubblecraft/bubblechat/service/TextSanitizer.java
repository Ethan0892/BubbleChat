package dev.bubblecraft.bubblechat.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextSanitizer {

    private TextSanitizer() {
    }

    public static String stripLegacySectionColors(String input) {
        if (input == null) return "";
        // remove §<code>
        return input.replaceAll("(?i)§[0-9A-FK-ORX]", "");
    }

    /**
     * Strips legacy ampersand colour/formatting codes from a string.
     * This is a best-effort filter and intentionally does not attempt to fully validate sequences.
     */
    public static String stripLegacyAmpersandColors(String input) {
        if (input == null) return "";
        // remove &<code> and the common hex syntaxes
        String out = input.replaceAll("(?i)&[0-9A-FK-ORX]", "");
        out = out.replaceAll("(?i)&#[0-9A-F]{6}", "");
        // also strip &x&F&F... style
        out = out.replaceAll("(?i)&x(&[0-9A-F]){6}", "");
        return out;
    }

    /**
     * Converts "&#RRGGBB" into the legacy hex format "&x&R&R&G&G&B&B".
     *
     * This allows Adventure's {@code LegacyComponentSerializer} to parse hex colours.
     */
    public static String convertAmpersandHexToLegacyXFormat(String input) {
        if (input == null) return "";

        Pattern p = Pattern.compile("(?i)&#([0-9a-f]{6})");
        Matcher m = p.matcher(input);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String hex = m.group(1);
            StringBuilder b = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); i++) {
                b.append('&').append(hex.charAt(i));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(b.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }

    public static String normalizeForDuplicateCheck(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
