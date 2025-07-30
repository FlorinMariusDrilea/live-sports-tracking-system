package com.sports.livesportstrackingsystem.kafka;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import com.sports.livesportstrackingsystem.model.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, EventMessage> kafkaTemplate;
    @Value("${app.kafka.topic.sports-events}")
    private String topic;

    public KafkaProducerService(KafkaTemplate<String, EventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(EventExternalApiResponse eventExternalApiResponse) {
        EventMessage message = EventMessage.builder()
                .eventId(eventExternalApiResponse.getEventId())
                .currentScore(eventExternalApiResponse.getCurrentScore())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        CompletableFuture<SendResult<String, EventMessage>> future = kafkaTemplate.send(topic, message.getEventId(), message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully published message for eventId {} to topic {}. Offset: {}",
                        message.getEventId(), topic, result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish message for eventId {} to topic {}: {}",
                        message.getEventId(), topic, ex.getMessage(), ex);
            }
        });

    }
}
