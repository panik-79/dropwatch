package com.dropwatch.notify;

import com.dropwatch.core.domain.NotificationChannelEntity;
import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.domain.TrackerRule;

import com.dropwatch.core.repository.AlertEventRepository;
import com.dropwatch.core.repository.NotificationChannelRepository;
import com.dropwatch.notify.channel.telegram.TelegramNotificationChannel;
import com.dropwatch.notify.pipeline.AlertDeduplicationService;
import com.dropwatch.notify.pipeline.NotificationOrchestratorService;
import com.dropwatch.notify.rule.AnyDiscountEvaluator;
import com.dropwatch.notify.rule.BackInStockEvaluator;
import com.dropwatch.notify.rule.PercentageDropEvaluator;
import com.dropwatch.notify.rule.PriceThresholdEvaluator;
import com.dropwatch.notify.rule.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationPipelineTest {

    private NotificationOrchestratorService orchestratorService;
    private AlertEventRepository alertEventRepository;
    private NotificationChannelRepository notificationChannelRepository;

    @BeforeEach
    void setUp() {
        alertEventRepository = mock(AlertEventRepository.class);
        notificationChannelRepository = mock(NotificationChannelRepository.class);

        when(alertEventRepository.findByTrackerIdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(alertEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationChannelEntity chanEntity = NotificationChannelEntity.builder()
                .id("chan-1")
                .userId("user-1")
                .type(NotificationChannelEntity.ChannelType.TELEGRAM)
                .config(Map.of("chatId", "123456789"))
                .enabled(true)
                .build();
        when(notificationChannelRepository.findAllById(any())).thenReturn(List.of(chanEntity));

        RuleEngineService ruleEngine = new RuleEngineService(List.of(
                new PriceThresholdEvaluator(),
                new PercentageDropEvaluator(),
                new BackInStockEvaluator(),
                new AnyDiscountEvaluator()
        ));
        AlertDeduplicationService deduplicationService = new AlertDeduplicationService(alertEventRepository);
        TelegramNotificationChannel telegramChannel = new TelegramNotificationChannel();

        orchestratorService = new NotificationOrchestratorService(
                ruleEngine,
                deduplicationService,
                alertEventRepository,
                notificationChannelRepository,
                List.of(telegramChannel)
        );
    }

    @Test
    void testProcessPriceUpdateTriggerAndDispatch() {
        TrackerRule rule = new TrackerRule.TargetPriceRule("r1", new BigDecimal("800.00"));

        Tracker tracker = Tracker.builder()
                .id("trk-1")
                .userId("user-1")
                .productId("prod-1")
                .variantId("sku-1")
                .rules(List.of(rule))
                .channelIds(List.of("chan-1"))
                .active(true)
                .cooldownSeconds(3600)
                .createdAt(Instant.now())
                .build();

        orchestratorService.processPriceUpdate(
                tracker,
                "Roadster Men Navy Blue T-Shirt",
                "https://www.myntra.com/2297861/buy",
                new BigDecimal("1000.00"),
                new BigDecimal("750.00"),
                true,
                true
        );

        verify(alertEventRepository, times(1)).save(any());
    }
}
