package com.dropwatch.flags;

import com.dropwatch.core.domain.FeatureFlagDoc;
import com.dropwatch.core.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FeatureFlagEngineTest {

    private FeatureFlagEngine engine;
    private FeatureFlagRepository repository;
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        repository = mock(FeatureFlagRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        engine = new FeatureFlagEngine(repository, rabbitTemplate);
    }

    @Test
    void testFlagEvaluationDisabledByDefault() {
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());

        boolean enabled = engine.isEnabled("non.existent.flag", "user-123");
        assertFalse(enabled);
    }

    @Test
    void testFlagEvaluationWhitelistedUser() {
        FeatureFlagDoc flag = FeatureFlagDoc.builder()
                .key("experimental.scraper")
                .type(FeatureFlagDoc.FlagType.BOOLEAN)
                .enabled(true)
                .percentage(0.0)
                .allowList(List.of("user-VIP"))
                .build();
        when(repository.findByKey("experimental.scraper")).thenReturn(Optional.of(flag));

        assertTrue(engine.isEnabled("experimental.scraper", "user-VIP"));
        assertFalse(engine.isEnabled("experimental.scraper", "user-regular"));
    }
}
