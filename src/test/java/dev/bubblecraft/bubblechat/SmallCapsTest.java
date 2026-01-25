package dev.bubblecraft.bubblechat;

import dev.bubblecraft.bubblechat.util.SmallCaps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SmallCapsTest {

    @Test
    void convertsBasicLatin() {
        assertEquals("ʜᴇʟʟᴏ ᴡᴏʀʟᴅ", SmallCaps.toSmallCaps("Hello World"));
        assertEquals("ᴅɪꜱᴄᴏʀᴅ", SmallCaps.toSmallCaps("DISCORD"));
    }

    @Test
    void keepsPunctuationAndNumbers() {
        assertEquals("ʜᴇʟʟᴏ! 123", SmallCaps.toSmallCaps("Hello! 123"));
    }
}
