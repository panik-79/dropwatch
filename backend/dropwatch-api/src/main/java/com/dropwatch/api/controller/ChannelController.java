package com.dropwatch.api.controller;

import com.dropwatch.core.domain.AlertEvent;
import com.dropwatch.core.domain.NotificationChannelEntity;
import com.dropwatch.core.repository.NotificationChannelRepository;
import com.dropwatch.notify.channel.telegram.TelegramNotificationChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels")
@Tag(name = "Notification Channels", description = "Endpoints for managing notification channels and test alerts")
public class ChannelController {

    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    private final NotificationChannelRepository channelRepository;
    private final TelegramNotificationChannel telegramChannel;
    private final com.dropwatch.core.repository.TrackerRepository trackerRepository;
    private final com.dropwatch.api.service.ProductService productService;
    private final com.dropwatch.notify.pipeline.NotificationOrchestratorService notificationOrchestratorService;

    public ChannelController(
            NotificationChannelRepository channelRepository,
            TelegramNotificationChannel telegramChannel,
            com.dropwatch.core.repository.TrackerRepository trackerRepository,
            com.dropwatch.api.service.ProductService productService,
            com.dropwatch.notify.pipeline.NotificationOrchestratorService notificationOrchestratorService
    ) {
        this.channelRepository = channelRepository;
        this.telegramChannel = telegramChannel;
        this.trackerRepository = trackerRepository;
        this.productService = productService;
        this.notificationOrchestratorService = notificationOrchestratorService;
    }

    private String resolveUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            if (!principal.isBlank() && !"anonymousUser".equalsIgnoreCase(principal)) {
                return principal;
            }
        }
        return "usr-demo-123";
    }

    @GetMapping("/telegram/config")
    @Operation(summary = "Get user Telegram channel configuration")
    public ResponseEntity<Map<String, String>> getTelegramConfig(Authentication auth) {
        String userId = resolveUserId(auth);
        List<NotificationChannelEntity> channels = channelRepository.findByUserId(userId);
        
        String chatId = "";
        for (NotificationChannelEntity channel : channels) {
            if (channel.getType() == NotificationChannelEntity.ChannelType.TELEGRAM && channel.getConfig() != null) {
                chatId = channel.getConfig().getOrDefault("chatId", "");
                if (!chatId.isBlank()) break;
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("chatId", chatId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/telegram/config")
    @Operation(summary = "Save Telegram channel configuration")
    public ResponseEntity<Map<String, Object>> saveTelegramConfig(@RequestBody Map<String, String> body, Authentication auth) {
        String userId = resolveUserId(auth);
        String chatId = body.getOrDefault("chatId", "").trim();

        List<NotificationChannelEntity> channels = channelRepository.findByUserId(userId);
        NotificationChannelEntity telegramEntity = channels.stream()
                .filter(c -> c.getType() == NotificationChannelEntity.ChannelType.TELEGRAM)
                .findFirst()
                .orElse(null);

        if (telegramEntity == null) {
            telegramEntity = NotificationChannelEntity.builder()
                    .id("chan-telegram-" + userId)
                    .userId(userId)
                    .type(NotificationChannelEntity.ChannelType.TELEGRAM)
                    .enabled(true)
                    .verified(true)
                    .createdAt(Instant.now())
                    .config(new HashMap<>())
                    .build();
        }

        if (telegramEntity.getConfig() == null) {
            telegramEntity.setConfig(new HashMap<>());
        }
        telegramEntity.getConfig().put("chatId", chatId);
        telegramEntity.setEnabled(true);
        channelRepository.save(telegramEntity);

        // Also save generic fallback entity with id "chan-telegram" so default tracker channelIds work
        NotificationChannelEntity defaultEntity = channelRepository.findById("chan-telegram").orElse(null);
        if (defaultEntity == null) {
            defaultEntity = NotificationChannelEntity.builder()
                    .id("chan-telegram")
                    .userId(userId)
                    .type(NotificationChannelEntity.ChannelType.TELEGRAM)
                    .enabled(true)
                    .verified(true)
                    .createdAt(Instant.now())
                    .config(new HashMap<>())
                    .build();
        }
        if (defaultEntity.getConfig() == null) {
            defaultEntity.setConfig(new HashMap<>());
        }
        defaultEntity.getConfig().put("chatId", chatId);
        defaultEntity.setEnabled(true);
        channelRepository.save(defaultEntity);

        // Immediately re-evaluate active trackers for this user
        List<com.dropwatch.core.domain.Tracker> activeTrackers = trackerRepository.findByUserId(userId);
        for (com.dropwatch.core.domain.Tracker t : activeTrackers) {
            if (!t.isActive()) continue;
            try {
                com.dropwatch.api.dto.ProductDto.ProductResponse p = productService.getProductById(t.getProductId());
                if (p != null && p.variants() != null && !p.variants().isEmpty()) {
                    com.dropwatch.core.domain.Variant targetVar = p.variants().stream()
                            .filter(v -> t.getVariantId() != null && t.getVariantId().equalsIgnoreCase(v.getSiteSkuId()))
                            .findFirst()
                            .orElse(p.variants().get(0));

                    if (targetVar.getAttributes() != null && targetVar.getAttributes().containsKey("sellingPrice")) {
                        Object spObj = targetVar.getAttributes().get("sellingPrice");
                        if (spObj != null) {
                            java.math.BigDecimal currentPrice = new java.math.BigDecimal(String.valueOf(spObj));
                            String titleWithLabel = p.title();
                            if (targetVar.getLabel() != null && !targetVar.getLabel().isBlank() && !"Standard".equalsIgnoreCase(targetVar.getLabel())) {
                                titleWithLabel += " (" + targetVar.getLabel() + ")";
                            }
                            notificationOrchestratorService.processPriceUpdate(
                                    t,
                                    titleWithLabel,
                                    p.canonicalUrl(),
                                    currentPrice,
                                    currentPrice,
                                    true,
                                    true
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate tracker {} on config save: {}", t.getId(), e.getMessage());
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("chatId", chatId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/telegram/test-alert")
    @Operation(summary = "Send a test alert via Telegram")
    public ResponseEntity<Map<String, Object>> sendTestAlert(@RequestBody Map<String, String> body, Authentication auth) {
        String userId = resolveUserId(auth);
        String chatId = body.get("chatId");

        if (chatId == null || chatId.isBlank()) {
            List<NotificationChannelEntity> channels = channelRepository.findByUserId(userId);
            for (NotificationChannelEntity c : channels) {
                if (c.getType() == NotificationChannelEntity.ChannelType.TELEGRAM && c.getConfig() != null) {
                    chatId = c.getConfig().get("chatId");
                    if (chatId != null && !chatId.isBlank()) break;
                }
            }
        }

        if (chatId == null || chatId.isBlank()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Please enter a valid Telegram Chat ID");
            return ResponseEntity.badRequest().body(err);
        }

        NotificationChannelEntity configEntity = NotificationChannelEntity.builder()
                .userId(userId)
                .type(NotificationChannelEntity.ChannelType.TELEGRAM)
                .config(Map.of("chatId", chatId))
                .build();

        AlertEvent mockAlert = AlertEvent.builder()
                .id("test-alert-" + System.currentTimeMillis())
                .trackerId("tracker-demo-test")
                .ruleType("TARGET_PRICE")
                .priceAtTrigger(java.math.BigDecimal.valueOf(7999.0))
                .createdAt(Instant.now())
                .build();

        log.info("Attempting to send test alert to Telegram chatId={}", chatId);
        boolean sent = telegramChannel.sendAlert(
                mockAlert,
                configEntity,
                "Demo Flipkart Product (Test Alert)",
                "https://www.flipkart.com"
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", sent);
        response.put("message", sent ? "Test notification dispatched to Telegram bot!" : "Failed to dispatch test notification. Check bot token and Chat ID.");
        
        if (sent) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
