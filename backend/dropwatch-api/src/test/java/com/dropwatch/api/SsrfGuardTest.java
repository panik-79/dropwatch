package com.dropwatch.api;

import com.dropwatch.api.security.SsrfGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SsrfGuardTest {

    private SsrfGuard ssrfGuard;

    @BeforeEach
    void setUp() {
        ssrfGuard = new SsrfGuard();
    }

    @Test
    void testBlockLocalhostAndPrivateIPs() {
        assertFalse(ssrfGuard.isSafeUrl("http://localhost:8080/secret"));
        assertFalse(ssrfGuard.isSafeUrl("http://127.0.0.1/admin"));
        assertFalse(ssrfGuard.isSafeUrl("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void testAllowPublicMerchantUrls() {
        assertTrue(ssrfGuard.isSafeUrl("https://www.myntra.com/tshirts/roadster/roadster-men-navy-blue-solid-round-neck-t-shirt/2297861/buy"));
        assertTrue(ssrfGuard.isSafeUrl("https://www.flipkart.com/apple-iphone-15-black-128-gb/p/itm123456"));
    }
}
