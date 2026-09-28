package com.dropwatch.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitTopologyConfig {

    public static final String EX_SCRAPE_DIRECT = "dw.scrape.direct";
    public static final String EX_SCRAPE_DELAY = "dw.scrape.delay";
    public static final String EX_SCRAPE_DLX = "dw.scrape.dlx";
    public static final String EX_ALERT_DIRECT = "dw.alert.direct";
    public static final String EX_FLAGS_FANOUT = "dw.flags.fanout";

    public static final String QUEUE_SCRAPE_WORK = "dw.scrape.work";
    public static final String QUEUE_SCRAPE_DELAY_1M = "dw.scrape.delay.1m";
    public static final String QUEUE_SCRAPE_DELAY_5M = "dw.scrape.delay.5m";
    public static final String QUEUE_SCRAPE_DELAY_15M = "dw.scrape.delay.15m";
    public static final String QUEUE_PARKING_LOT = "dw.scrape.parking-lot";
    public static final String QUEUE_ALERT_DISPATCH = "dw.alert.dispatch";

    public static final String RK_WORK = "work";
    public static final String RK_DELAY_1M = "delay.1m";
    public static final String RK_DELAY_5M = "delay.5m";
    public static final String RK_DELAY_15M = "delay.15m";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Exchanges
    @Bean
    public DirectExchange scrapeDirectExchange() {
        return new DirectExchange(EX_SCRAPE_DIRECT, true, false);
    }

    @Bean
    public DirectExchange scrapeDelayExchange() {
        return new DirectExchange(EX_SCRAPE_DELAY, true, false);
    }

    @Bean
    public FanoutExchange scrapeDlxExchange() {
        return new FanoutExchange(EX_SCRAPE_DLX, true, false);
    }

    @Bean
    public DirectExchange alertDirectExchange() {
        return new DirectExchange(EX_ALERT_DIRECT, true, false);
    }

    @Bean
    public FanoutExchange flagsFanoutExchange() {
        return new FanoutExchange(EX_FLAGS_FANOUT, true, false);
    }

    // Queues
    @Bean
    public Queue scrapeWorkQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_WORK).build();
    }

    @Bean
    public Queue scrapeDelay1mQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_DELAY_1M)
                .withArguments(Map.of(
                        "x-message-ttl", 60000,
                        "x-dead-letter-exchange", EX_SCRAPE_DIRECT,
                        "x-dead-letter-routing-key", RK_WORK
                )).build();
    }

    @Bean
    public Queue scrapeDelay5mQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_DELAY_5M)
                .withArguments(Map.of(
                        "x-message-ttl", 300000,
                        "x-dead-letter-exchange", EX_SCRAPE_DIRECT,
                        "x-dead-letter-routing-key", RK_WORK
                )).build();
    }

    @Bean
    public Queue scrapeDelay15mQueue() {
        return QueueBuilder.durable(QUEUE_SCRAPE_DELAY_15M)
                .withArguments(Map.of(
                        "x-message-ttl", 900000,
                        "x-dead-letter-exchange", EX_SCRAPE_DIRECT,
                        "x-dead-letter-routing-key", RK_WORK
                )).build();
    }

    @Bean
    public Queue parkingLotQueue() {
        return QueueBuilder.durable(QUEUE_PARKING_LOT).build();
    }

    @Bean
    public Queue alertDispatchQueue() {
        return QueueBuilder.durable(QUEUE_ALERT_DISPATCH).build();
    }

    // Bindings
    @Bean
    public Binding workBinding() {
        return BindingBuilder.bind(scrapeWorkQueue()).to(scrapeDirectExchange()).with(RK_WORK);
    }

    @Bean
    public Binding delay1mBinding() {
        return BindingBuilder.bind(scrapeDelay1mQueue()).to(scrapeDelayExchange()).with(RK_DELAY_1M);
    }

    @Bean
    public Binding delay5mBinding() {
        return BindingBuilder.bind(scrapeDelay5mQueue()).to(scrapeDelayExchange()).with(RK_DELAY_5M);
    }

    @Bean
    public Binding delay15mBinding() {
        return BindingBuilder.bind(scrapeDelay15mQueue()).to(scrapeDelayExchange()).with(RK_DELAY_15M);
    }

    @Bean
    public Binding parkingLotBinding() {
        return BindingBuilder.bind(parkingLotQueue()).to(scrapeDlxExchange());
    }

    @Bean
    public Binding alertDispatchBinding() {
        return BindingBuilder.bind(alertDispatchQueue()).to(alertDirectExchange()).with("alert.dispatch");
    }
}
