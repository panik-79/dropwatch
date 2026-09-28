package com.dropwatch.scraper;

import com.dropwatch.core.domain.Site;
import com.dropwatch.scraper.provider.myntra.MyntraParser;
import com.dropwatch.scraper.spi.ProductSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MyntraParserTest {

    private MyntraParser parser;

    @BeforeEach
    void setUp() {
        parser = new MyntraParser();
    }

    @Test
    void testParseMyntraSampleHtml() throws Exception {
        InputStream is = getClass().getResourceAsStream("/fixtures/myntra_sample.html");
        assertNotNull(is, "myntra_sample.html fixture missing");
        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        ProductSnapshot snapshot = parser.parse(html, "https://www.myntra.com/2297861/buy", "2297861");

        assertNotNull(snapshot);
        assertEquals(Site.MYNTRA, snapshot.site());
        assertEquals("2297861", snapshot.siteProductId());
        assertTrue(snapshot.title().contains("Roadster"));
        assertEquals("Roadster", snapshot.brand());
        assertFalse(snapshot.variants().isEmpty());

        var firstVariant = snapshot.variants().get(0);
        assertEquals("S", firstVariant.label());
        assertEquals(399.0, firstVariant.sellingPrice().doubleValue());
        assertEquals(699.0, firstVariant.mrp().doubleValue());
        assertTrue(firstVariant.inStock());
    }
}
