package com.dropwatch.app;

import com.dropwatch.core.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

@SpringBootTest
@ActiveProfiles("test")
class DropWatchApplicationTest {

    @TestConfiguration
    static class TestBeansConfig {

        @Bean
        @Primary
        public MongoMappingContext mongoMappingContext() {
            MongoMappingContext mappingContext = new MongoMappingContext();
            mappingContext.setInitialEntitySet(Set.of(
                    User.class, Product.class, Variant.class, Tracker.class,
                    PriceSnapshot.class, AlertEvent.class, NotificationChannelEntity.class,
                    FeatureFlagDoc.class, ScrapeRun.class
            ));
            mappingContext.afterPropertiesSet();
            return mappingContext;
        }

        @Bean
        @Primary
        public MongoTemplate mongoTemplate(MongoMappingContext mappingContext) {
            MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
            converter.afterPropertiesSet();

            MongoTemplate template = Mockito.mock(MongoTemplate.class);
            Mockito.when(template.getConverter()).thenReturn(converter);
            return template;
        }

        @Bean
        @Primary
        public ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }

        @Bean
        @Primary
        public RabbitTemplate rabbitTemplate() {
            return Mockito.mock(RabbitTemplate.class);
        }
    }

    @Test
    void contextLoads() {
        // Verifies Spring Boot context bootstraps cleanly
    }
}
