package com.dropwatch.flags;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlagQueueConfig {

    public static final String FANOUT_EXCHANGE = "dw.flags.fanout";
    public static final String FLAG_QUEUE_NAME = "dw.flags.sync";

    @Bean
    public FanoutExchange flagFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue flagQueue() {
        return QueueBuilder.durable(FLAG_QUEUE_NAME).build();
    }

    @Bean
    public Binding flagBinding(Queue flagQueue, FanoutExchange flagFanoutExchange) {
        return BindingBuilder.bind(flagQueue).to(flagFanoutExchange);
    }
}
