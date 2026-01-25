package dev.bubblecraft.bubblechat;

import dev.bubblecraft.bubblechat.service.TextSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextSanitizerTest {

    @Test
    void convertAmpersandHexToLegacyXFormat_convertsAllOccurrences() {
        String in = "hello &#12abef world &#000000!";
        String out = TextSanitizer.convertAmpersandHexToLegacyXFormat(in);
        assertEquals("hello &x&1&2&a&b&e&f world &x&0&0&0&0&0&0!", out);
    }

    @Test
    void stripLegacyAmpersandColors_stripsBasicAndHexFormats() {
        String in = "&aGreen &lBold &#12abefHex &x&1&2&a&b&e&fAlsoHex &rReset";
        String out = TextSanitizer.stripLegacyAmpersandColors(in);
        assertEquals("Green Bold Hex AlsoHex Reset", out);
    }
}
