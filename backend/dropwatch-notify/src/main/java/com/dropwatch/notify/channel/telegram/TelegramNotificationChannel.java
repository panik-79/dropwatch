package com.dropwatch.notify.channel.telegram;

import com.dropwatch.core.domain.AlertEvent;
import com.dropwatch.core.domain.NotificationChannelEntity;
import com.dropwatch.notify.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationChannel.class);

    private final RestTemplate restTemplate;

    @Value("${dropwatch.notify.telegram.bot-token:${dropwatch.telegram.bot-token:}}")
    private String botToken;

    public TelegramNotificationChannel() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public NotificationChannelEntity.ChannelType getChannelType() {
        return NotificationChannelEntity.ChannelType.TELEGRAM;
    }

    @Override
    public boolean sendAlert(AlertEvent alert, NotificationChannelEntity config, String productTitle, String buyUrl) {
        String chatId = config.getConfig() != null ? config.getConfig().get("chatId") : null;
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram alert skipped: missing target chatId in channel config");
            return false;
        }

        String formattedMessage = buildTelegramMessage(alert, productTitle, buyUrl);

        if (botToken == null || botToken.isBlank()) {
            log.info("[MOCK TELEGRAM ALERT] to chatId={}: \n{}", chatId, formattedMessage);
            return true;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", formattedMessage);
            body.put("parse_mode", "MarkdownV2");
            body.put("disable_web_page_preview", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);

            log.info("Telegram alert successfully sent to chatId={}", chatId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send Telegram alert to chatId={}: {}", chatId, e.getMessage(), e);
            return false;
        }
    }

    private String buildTelegramMessage(AlertEvent alert, String productTitle, String buyUrl) {
        String escTitle = TelegramMarkdownEscaper.escapeMarkdownV2(productTitle);
        String escRule = TelegramMarkdownEscaper.escapeMarkdownV2(alert.getRuleType());
        String escPrice = TelegramMarkdownEscaper.escapeMarkdownV2("₹" + alert.getPriceAtTrigger());
        String escUrl = buyUrl != null ? buyUrl : "https://dropwatch.com";

        return """
                🚨 *DROPWATCH PRICE ALERT* 🚨
                
                *Product:* %s
                *Rule Triggered:* %s
                
                🔥 *Trigger Price:* *%s*
                
                🛒 [Buy Now on Merchant](%s)
                """.formatted(escTitle, escRule, escPrice, escUrl);
    }
}
