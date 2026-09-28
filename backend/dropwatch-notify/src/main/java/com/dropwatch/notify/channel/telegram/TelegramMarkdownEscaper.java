package com.dropwatch.notify.channel.telegram;

public final class TelegramMarkdownEscaper {

    private TelegramMarkdownEscaper() {}

    /**
     * Escapes special characters for Telegram MarkdownV2 formatting.
     * Characters escaped: _ * [ ] ( ) ~ ` > # + - = | { } . !
     */
    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";
        return text.replaceAll("([_*\\[\\]()~`>#+\\-=|{}.!])", "\\\\$1");
    }
}
