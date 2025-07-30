package com.sports.livesportstrackingsystem.scheduler;

import com.sports.livesportstrackingsystem.external.ExternalApiService;
import com.sports.livesportstrackingsystem.kafka.KafkaProducerService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.time.Duration;

@Slf4j
@Component
public class EventScheduler {
    private final ThreadPoolTaskScheduler taskScheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ExternalApiService externalApiServiceClient;
    private final KafkaProducerService kafkaProducerService;

    // 10 seconds
    @Value("${app.polling.interval.seconds:10}")
    private long pollingIntervalSeconds;

    public EventScheduler(ExternalApiService externalApiServiceClient, KafkaProducerService kafkaProducerService) {
        this.externalApiServiceClient = externalApiServiceClient;
        this.kafkaProducerService = kafkaProducerService;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(5);
        this.taskScheduler.setThreadNamePrefix("event-scheduler-");
    }

    @PostConstruct
    public void init() {
        taskScheduler.initialize();
        // The value will be available here because @Value injection happens before @PostConstruct
        log.info("Event Polling Scheduler initialized with polling interval: {} seconds", pollingIntervalSeconds);
    }

    public void startScheduler(String eventId) {
        // start schedule if it's not started already
        scheduledTasks.computeIfAbsent(eventId, k -> {
            log.info("Scheduling polling for eventId: {}", eventId);
            return taskScheduler.scheduleAtFixedRate(() -> fetchAndPublishEventData(eventId), Duration.ofSeconds(pollingIntervalSeconds));
        });
    }

    public void stopScheduler(String eventId) {
        ScheduledFuture<?> future = scheduledTasks.remove(eventId);
        if (future != null) {
            future.cancel(true);
            log.info("Polling stopped for event {}", eventId);
        }
    }

    public void fetchAndPublishEventData(String eventId) {
        log.debug("Schedule event: {}", eventId);
        try {
            externalApiServiceClient.getEventScore(eventId)
                    .doOnSuccess(eventExternalApi -> {
                        log.debug("Received API response for event {}: {}", eventId, eventExternalApi);
                        kafkaProducerService.publish(eventExternalApi);
                    })
                    .doOnError(error -> log.error("Error polling event {}: {}", eventId, error.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Unexpected error during scheduling for event {}: {}", eventId, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutDown() {
        taskScheduler.shutdown();
        log.info("Scheduler shutting down.");
    }
}
