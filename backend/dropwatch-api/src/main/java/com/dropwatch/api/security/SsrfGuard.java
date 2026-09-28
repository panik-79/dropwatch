package com.dropwatch.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class SsrfGuard {

    private static final Logger log = LoggerFactory.getLogger(SsrfGuard.class);

    public boolean isSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(urlString.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.warn("Blocked URL with invalid scheme: {}", scheme);
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            if (host.equalsIgnoreCase("localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
                log.warn("Blocked internal hostname: {}", host);
                return false;
            }

            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress()) {
                    log.warn("Blocked SSRF attempt targeting private IP: {} ({})", addr.getHostAddress(), host);
                    return false;
                }

                // Check cloud metadata IPv4 169.254.169.254
                if ("169.254.169.254".equals(addr.getHostAddress())) {
                    log.warn("Blocked SSRF attempt targeting cloud metadata IP: {}", host);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("SSRF Guard failed to validate URL {}: {}", urlString, e.getMessage());
            return false;
        }
    }
}
