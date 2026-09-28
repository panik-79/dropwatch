package com.dropwatch.scraper;

import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.provider.flipkart.FlipkartParser;
import com.dropwatch.scraper.spi.ProductSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FlipkartParserTest {

    private FlipkartParser parser;

    @BeforeEach
    void setUp() {
        parser = new FlipkartParser();
    }

    @Test
    void testParseFlipkartSampleHtml() throws Exception {
        InputStream is = getClass().getResourceAsStream("/fixtures/flipkart_sample.html");
        assertNotNull(is, "flipkart_sample.html fixture missing");
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        ProductSnapshot snapshot = parser.parse(html, "https://www.flipkart.com/p/itm123456", "itm123456");

        assertNotNull(snapshot);
        assertEquals(Site.FLIPKART, snapshot.site());
        assertEquals("itm123456", snapshot.siteProductId());
        assertTrue(snapshot.title().contains("iPhone 15"));
        assertEquals("Apple", snapshot.brand());
        assertFalse(snapshot.variants().isEmpty());

        var firstVariant = snapshot.variants().get(0);
        assertEquals(65999.0, firstVariant.sellingPrice().doubleValue());
        assertTrue(firstVariant.inStock());
    }
}
