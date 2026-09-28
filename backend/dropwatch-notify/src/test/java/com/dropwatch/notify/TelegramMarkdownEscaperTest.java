package com.dropwatch.notify;

import com.dropwatch.notify.channel.telegram.TelegramMarkdownEscaper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelegramMarkdownEscaperTest {

    @Test
    void testEscapeSpecialCharacters() {
        String input = "Roadster Men's T-Shirt (Navy Blue, Size: M) - 50% OFF! Price: ₹399.00 [Buy Now]";
        String escaped = TelegramMarkdownEscaper.escapeMarkdownV2(input);

        assertTrue(escaped.contains("\\-"));
        assertTrue(escaped.contains("\\("));
        assertTrue(escaped.contains("\\)"));
        assertTrue(escaped.contains("\\!"));
        assertTrue(escaped.contains("\\["));
        assertTrue(escaped.contains("\\]"));
        assertTrue(escaped.contains("\\."));
    }
}
