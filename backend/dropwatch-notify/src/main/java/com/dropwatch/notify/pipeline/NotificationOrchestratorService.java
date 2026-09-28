package com.dropwatch.notify.pipeline;

import com.dropwatch.core.domain.AlertEvent;
import com.dropwatch.core.domain.NotificationChannelEntity;
import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.repository.AlertEventRepository;
import com.dropwatch.core.repository.NotificationChannelRepository;
import com.dropwatch.notify.channel.NotificationChannel;
import com.dropwatch.notify.rule.RuleEngineService;
import com.dropwatch.notify.rule.RuleEvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestratorService.class);

    private final RuleEngineService ruleEngineService;
    private final AlertDeduplicationService deduplicationService;
    private final AlertEventRepository alertEventRepository;
    private final NotificationChannelRepository notificationChannelRepository;
    private final Map<String, NotificationChannel> channels;

    public NotificationOrchestratorService(
            RuleEngineService ruleEngineService,
            AlertDeduplicationService deduplicationService,
            AlertEventRepository alertEventRepository,
            NotificationChannelRepository notificationChannelRepository,
            List<NotificationChannel> channelList
    ) {
        this.ruleEngineService = ruleEngineService;
        this.deduplicationService = deduplicationService;
        this.alertEventRepository = alertEventRepository;
        this.notificationChannelRepository = notificationChannelRepository;
        this.channels = channelList.stream()
                .collect(Collectors.toMap(c -> c.getChannelType().name(), Function.identity()));
    }

    public void processPriceUpdate(
            Tracker tracker,
            String productTitle,
            String buyUrl,
            BigDecimal previousPrice,
            BigDecimal currentPrice,
            boolean previouslyInStock,
            boolean currentlyInStock
    ) {
        if (tracker == null || tracker.getRules() == null || tracker.getRules().isEmpty()) {
            return;
        }

        Optional<RuleEvaluationResult> evalOpt = ruleEngineService.evaluateRules(
                tracker.getRules(), previousPrice, currentPrice, previouslyInStock, currentlyInStock
        );

        if (evalOpt.isEmpty() || !evalOpt.get().triggered()) {
            return;
        }

        RuleEvaluationResult result = evalOpt.get();

        if (deduplicationService.isDuplicateAlert(tracker.getId(), tracker.getCooldownSeconds())) {
            log.info("Alert deduplicated/suppressed for trackerId={}", tracker.getId());
            return;
        }

        AlertEvent alertEvent = AlertEvent.builder()
                .trackerId(tracker.getId())
                .ruleType(result.ruleType())
                .priceAtTrigger(result.newPrice())
                .deliveryStatus(AlertEvent.DeliveryStatus.PENDING)
                .dedupeKey(tracker.getId() + "-" + System.currentTimeMillis())
                .createdAt(Instant.now())
                .build();

        AlertEvent savedEvent = alertEventRepository.save(alertEvent);

        // Fetch user's notification channels with fallback resolution
        List<NotificationChannelEntity> channelEntities = new java.util.ArrayList<>();
        if (tracker.getChannelIds() != null && !tracker.getChannelIds().isEmpty()) {
            channelEntities.addAll(notificationChannelRepository.findAllById(tracker.getChannelIds()));
        }
        if (channelEntities.isEmpty() && tracker.getUserId() != null) {
            channelEntities.addAll(notificationChannelRepository.findByUserId(tracker.getUserId()));
        }
        if (channelEntities.isEmpty()) {
            notificationChannelRepository.findById("chan-telegram").ifPresent(channelEntities::add);
        }

        if (channelEntities.isEmpty()) {
            log.warn("No configured notification channel found for trackerId={} (userId={}). Please set Telegram Chat ID in Settings.", tracker.getId(), tracker.getUserId());
            return;
        }

        for (NotificationChannelEntity channelConfig : channelEntities) {
            if (!channelConfig.isEnabled()) continue;

            NotificationChannel channel = channels.get(channelConfig.getType().name());
            if (channel != null) {
                boolean success = channel.sendAlert(savedEvent, channelConfig, productTitle, buyUrl);
                log.info("Dispatched alert for trackerId={} to channel {}: success={}", tracker.getId(), channelConfig.getType(), success);
            } else {
                log.warn("No handler found for notification channel type: {}", channelConfig.getType());
            }
        }
    }
}
