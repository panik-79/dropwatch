package com.dropwatch.core.domain;

public enum Site {
    MYNTRA,
    FLIPKART,
    AMAZON,
    OTHER;

    public static Site fromUrl(String url) {
        if (url == null) return OTHER;
        String lower = url.toLowerCase();
        if (lower.contains("myntra.com") || lower.contains("mynt.in")) return MYNTRA;
        if (lower.contains("flipkart.com") || lower.contains("fkrt.it") || lower.contains("dl.flipkart.com")) return FLIPKART;
        if (lower.contains("amazon.in") || lower.contains("amazon.com") || lower.contains("amzn.to")) return AMAZON;
        return OTHER;
    }
}
