package dev.bubblecraft.bubblechat.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SmallCaps {

    private static final Map<Character, String> MAP = new HashMap<>();

    static {
        // Basic Latin a-z. Where a true small-caps variant is uncommon, we fall back to the normal letter.
        MAP.put('a', "ᴀ");
        MAP.put('b', "ʙ");
        MAP.put('c', "ᴄ");
        MAP.put('d', "ᴅ");
        MAP.put('e', "ᴇ");
        MAP.put('f', "ꜰ");
        MAP.put('g', "ɢ");
        MAP.put('h', "ʜ");
        MAP.put('i', "ɪ");
        MAP.put('j', "ᴊ");
        MAP.put('k', "ᴋ");
        MAP.put('l', "ʟ");
        MAP.put('m', "ᴍ");
        MAP.put('n', "ɴ");
        MAP.put('o', "ᴏ");
        MAP.put('p', "ᴘ");
        MAP.put('q', "ǫ");
        MAP.put('r', "ʀ");
        MAP.put('s', "ꜱ");
        MAP.put('t', "ᴛ");
        MAP.put('u', "ᴜ");
        MAP.put('v', "ᴠ");
        MAP.put('w', "ᴡ");
        MAP.put('x', "x");
        MAP.put('y', "ʏ");
        MAP.put('z', "ᴢ");
    }

    private SmallCaps() {
    }

    public static String toSmallCaps(String input) {
        if (input == null || input.isEmpty()) return input;

        String lower = input.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            String mapped = MAP.get(c);
            out.append(mapped == null ? c : mapped);
        }

        return out.toString();
    }

    /**
     * Converts text to small caps while keeping MiniMessage tags intact.
     * Example: "<gray>Hello</gray>" -> "<gray>ʜᴇʟʟᴏ</gray>"
     */
    public static String toSmallCapsMiniMessage(String input) {
        if (input == null || input.isEmpty()) return input;

        String lower = input.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());

        boolean inTag = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '<') {
                inTag = true;
                out.append(c);
                continue;
            }
            if (c == '>') {
                inTag = false;
                out.append(c);
                continue;
            }

            if (inTag) {
                out.append(c);
                continue;
            }

            String mapped = MAP.get(c);
            out.append(mapped == null ? c : mapped);
        }

        return out.toString();
    }
}
