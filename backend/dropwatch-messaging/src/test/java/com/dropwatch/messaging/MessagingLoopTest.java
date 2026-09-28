package com.dropwatch.messaging;

import com.dropwatch.core.domain.Tracker;
import com.dropwatch.core.repository.TrackerRepository;
import com.dropwatch.messaging.config.RabbitTopologyConfig;
import com.dropwatch.messaging.consumer.ScrapeTaskConsumer;
import com.dropwatch.messaging.dto.ScrapeTask;
import com.dropwatch.messaging.producer.ScrapeTaskProducer;
import com.dropwatch.messaging.reconciler.ScrapeLoopReconciler;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class MessagingLoopTest {

    @Test
    @DisplayName("ScrapeTask creation and nextAttempt incrementing")
    void scrapeTaskCreation() {
        ScrapeTask task = ScrapeTask.create("prod-123", List.of("var-1", "var-2"), "trace-abc");
        assertNotNull(task.taskId());
        assertEquals("prod-123", task.productId());
        assertEquals(1, task.attempt());
        assertEquals("trace-abc", task.traceId());

        ScrapeTask next = task.nextAttempt();
        assertEquals(2, next.attempt());
        assertEquals("trace-abc", next.traceId());
        assertNotEquals(task.taskId(), next.taskId());
    }

    @Test
    @DisplayName("Consumer routes successful task to delay queue")
    void consumerSuccessRouting() throws IOException {
        ScrapeTaskProducer producer = Mockito.mock(ScrapeTaskProducer.class);
        ScrapeTaskConsumer consumer = new ScrapeTaskConsumer(producer);
        Channel channel = Mockito.mock(Channel.class);

        ScrapeTask task = ScrapeTask.create("prod-123", List.of("var-1"), "trace-1");

        consumer.consume(task, channel, 1L);

        Mockito.verify(producer).publishToDelayQueue(any(ScrapeTask.class), eq(RabbitTopologyConfig.RK_DELAY_5M));
        Mockito.verify(channel).basicAck(1L, false);
    }

    @Test
    @DisplayName("Consumer routes failed task reaching max attempts to parking lot")
    void consumerParkingLotRouting() throws IOException {
        ScrapeTaskProducer producer = Mockito.mock(ScrapeTaskProducer.class);
        ScrapeTaskConsumer consumer = new ScrapeTaskConsumer(producer);
        consumer.setTaskExecutionDelegate(t -> false); // Always fails

        Channel channel = Mockito.mock(Channel.class);
        ScrapeTask maxTask = new ScrapeTask("task-max", "prod-123", List.of("var-1"), 5, "trace-1", java.time.Instant.now());

        consumer.consume(maxTask, channel, 2L);

        Mockito.verify(producer).publishToParkingLot(maxTask);
        Mockito.verify(channel).basicAck(2L, false);
    }

    @Test
    @DisplayName("Reconciler fans-in multiple trackers for same product into single ScrapeTask")
    void reconcilerFanInGrouping() {
        TrackerRepository repo = Mockito.mock(TrackerRepository.class);
        ScrapeTaskProducer producer = Mockito.mock(ScrapeTaskProducer.class);

        Tracker t1 = Tracker.builder().id("t1").productId("prod-nike").variantId("var-uk8").active(true).build();
        Tracker t2 = Tracker.builder().id("t2").productId("prod-nike").variantId("var-uk9").active(true).build();

        Mockito.when(repo.findByActiveTrue()).thenReturn(List.of(t1, t2));

        ScrapeLoopReconciler reconciler = new ScrapeLoopReconciler(repo, producer);
        reconciler.reconcileActiveTrackers();

        Mockito.verify(producer, Mockito.times(1)).publishToWorkQueue(any(ScrapeTask.class));
    }
}
