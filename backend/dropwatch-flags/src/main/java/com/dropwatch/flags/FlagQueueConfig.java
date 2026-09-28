package com.dropwatch.flags;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlagQueueConfig {

    public static final String FANOUT_EXCHANGE = "dw.flags.fanout";

    @Bean
    public FanoutExchange flagFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue flagQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding flagBinding(Queue flagQueue, FanoutExchange flagFanoutExchange) {
        return BindingBuilder.bind(flagQueue).to(flagFanoutExchange);
    }
}
