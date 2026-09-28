package com.dropwatch.flags;

import com.dropwatch.core.domain.FeatureFlagDoc;
import com.dropwatch.core.repository.FeatureFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FeatureFlagEngine {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagEngine.class);

    private final FeatureFlagRepository flagRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Map<String, FeatureFlagDoc> localCache = new ConcurrentHashMap<>();

    public FeatureFlagEngine(FeatureFlagRepository flagRepository, RabbitTemplate rabbitTemplate) {
        this.flagRepository = flagRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public boolean isEnabled(String flagKey, String userId) {
        FeatureFlagDoc flag = localCache.computeIfAbsent(flagKey, key ->
                flagRepository.findByKey(key).orElseGet(() -> createDefaultFlag(key))
        );

        if (!flag.isEnabled()) {
            return false;
        }

        // Check user allowList
        if (userId != null && flag.getAllowList() != null && flag.getAllowList().contains(userId)) {
            return true;
        }

        // Check percentage rollout
        Double pct = flag.getPercentage();
        if (userId != null && pct != null && pct < 100.0) {
            int hash = Math.abs((flagKey + ":" + userId).hashCode()) % 100;
            return hash < pct;
        }

        return pct == null || pct > 0.0;
    }

    public FeatureFlagDoc saveOrUpdateFlag(FeatureFlagDoc flag) {
        FeatureFlagDoc saved = flagRepository.save(flag);
        localCache.put(saved.getKey(), saved);

        // Broadcast invalidation across cluster via RabbitMQ fanout
        try {
            rabbitTemplate.convertAndSend("dw.flags.fanout", "", new FlagInvalidationMessage(saved.getKey(), System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("Failed to broadcast flag invalidation for key {}: {}", saved.getKey(), e.getMessage());
        }

        return saved;
    }

    @RabbitListener(queues = "#{flagQueue.name}")
    public void onFlagInvalidation(FlagInvalidationMessage msg) {
        if (msg != null && msg.flagKey() != null) {
            log.info("Invalidating local feature flag cache for key: {}", msg.flagKey());
            localCache.remove(msg.flagKey());
        }
    }

    public void invalidateCache(String flagKey) {
        localCache.remove(flagKey);
    }

    private FeatureFlagDoc createDefaultFlag(String key) {
        return FeatureFlagDoc.builder()
                .key(key)
                .description("Default feature flag")
                .type(FeatureFlagDoc.FlagType.BOOLEAN)
                .enabled(false)
                .percentage(0.0)
                .build();
    }
}
