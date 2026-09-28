package com.dropwatch.core.domain;

import java.io.Serializable;

public record UserPreferences(
        String currency,
        String timezone,
        QuietHours quietHours
) implements Serializable {

    public record QuietHours(
            boolean enabled,
            int startHour, // 0-23
            int endHour,   // 0-23
            boolean allowFlashSaleOverride
    ) implements Serializable {
        public static QuietHours defaultHours() {
            return new QuietHours(false, 23, 7, true);
        }
    }

    public static UserPreferences defaultPreferences() {
        return new UserPreferences("INR", "Asia/Kolkata", QuietHours.defaultHours());
    }
}
