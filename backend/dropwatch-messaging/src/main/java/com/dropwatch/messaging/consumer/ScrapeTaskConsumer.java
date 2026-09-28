package com.dropwatch.messaging.consumer;

import com.dropwatch.messaging.config.RabbitTopologyConfig;
import com.dropwatch.messaging.dto.ScrapeTask;
import com.dropwatch.messaging.producer.ScrapeTaskProducer;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScrapeTaskConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScrapeTaskConsumer.class);
    private static final int MAX_ATTEMPTS = 5;

    private final Set<String> processedTaskIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ScrapeTaskProducer producer;
    private TaskExecutionDelegate taskExecutionDelegate;

    public interface TaskExecutionDelegate {
        boolean execute(ScrapeTask task);
    }

    public ScrapeTaskConsumer(ScrapeTaskProducer producer) {
        this.producer = producer;
        // Default dummy delegate until Scraper module binds
        this.taskExecutionDelegate = task -> true;
    }

    public void setTaskExecutionDelegate(TaskExecutionDelegate delegate) {
        this.taskExecutionDelegate = delegate;
    }

    @RabbitListener(queues = RabbitTopologyConfig.QUEUE_SCRAPE_WORK)
    public void consume(ScrapeTask task, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Received ScrapeTask from work queue: productId={}, taskId={}, attempt={}",
                task.productId(), task.taskId(), task.attempt());

        // Idempotency check
        if (processedTaskIds.contains(task.taskId())) {
            log.warn("Duplicate ScrapeTask detected, skipping execution: taskId={}", task.taskId());
            channel.basicAck(tag, false);
            return;
        }

        processedTaskIds.add(task.taskId());

        try {
            boolean success = taskExecutionDelegate.execute(task);

            if (success) {
                log.info("ScrapeTask completed successfully: productId={}", task.productId());
            } else {
                handleFailure(task);
            }

            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Unhandled error processing ScrapeTask: taskId={}", task.taskId(), e);
            handleFailure(task);
            channel.basicAck(tag, false);
        }
    }

    private void handleFailure(ScrapeTask task) {
        if (task.attempt() >= MAX_ATTEMPTS) {
            log.error("ScrapeTask reached max attempt limit ({}) for productId={}. Routing to parking lot.",
                    MAX_ATTEMPTS, task.productId());
            producer.publishToParkingLot(task);
        } else {
            String delayRoutingKey = task.attempt() >= 3 ? RabbitTopologyConfig.RK_DELAY_15M : RabbitTopologyConfig.RK_DELAY_5M;
            log.warn("ScrapeTask failed for productId={}, attempt={}. Re-routing to delay queue: {}",
                    task.productId(), task.attempt(), delayRoutingKey);
            producer.publishToDelayQueue(task.nextAttempt(), delayRoutingKey);
        }
    }
}
