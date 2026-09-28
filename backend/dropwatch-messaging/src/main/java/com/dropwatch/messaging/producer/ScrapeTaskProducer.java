package com.dropwatch.messaging.producer;

import com.dropwatch.messaging.config.RabbitTopologyConfig;
import com.dropwatch.messaging.dto.PriceObservedEvent;
import com.dropwatch.messaging.dto.ScrapeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScrapeTaskProducer {

    private static final Logger log = LoggerFactory.getLogger(ScrapeTaskProducer.class);

    private final RabbitTemplate rabbitTemplate;

    public ScrapeTaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishToWorkQueue(ScrapeTask task) {
        log.info("Publishing ScrapeTask to work queue: productId={}, taskId={}, attempt={}",
                task.productId(), task.taskId(), task.attempt());
        rabbitTemplate.convertAndSend(
                RabbitTopologyConfig.EX_SCRAPE_DIRECT,
                RabbitTopologyConfig.RK_WORK,
                task
        );
    }

    public void publishToDelayQueue(ScrapeTask task, String delayRoutingKey) {
        log.info("Publishing ScrapeTask to delay exchange: routingKey={}, productId={}, attempt={}",
                delayRoutingKey, task.productId(), task.attempt());
        rabbitTemplate.convertAndSend(
                RabbitTopologyConfig.EX_SCRAPE_DELAY,
                delayRoutingKey,
                task
        );
    }

    public void publishToParkingLot(ScrapeTask task) {
        log.warn("Routing poison ScrapeTask to parking lot: productId={}, taskId={}, attempt={}",
                task.productId(), task.taskId(), task.attempt());
        rabbitTemplate.convertAndSend(
                RabbitTopologyConfig.EX_SCRAPE_DLX,
                "",
                task
        );
    }

    public void publishPriceObserved(PriceObservedEvent event) {
        log.info("Publishing PriceObservedEvent: eventId={}, productId={}, snapshotsCount={}",
                event.eventId(), event.product() != null ? event.product().getId() : "null", event.snapshots().size());
        rabbitTemplate.convertAndSend(
                RabbitTopologyConfig.EX_ALERT_DIRECT,
                "alert.dispatch",
                event
        );
    }
}
