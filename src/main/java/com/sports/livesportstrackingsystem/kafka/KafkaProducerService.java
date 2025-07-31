package com.sports.livesportstrackingsystem.kafka;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class KafkaProducerService {
    private final KafkaTemplate<String, EventExternalApiResponse> kafkaTemplate;
    @Value("${app.kafka.topic.sports-events}")
    private String topic;

    public KafkaProducerService(KafkaTemplate<String, EventExternalApiResponse> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(EventExternalApiResponse eventExternalApiResponse) {
        EventExternalApiResponse message = EventExternalApiResponse.builder()
                .eventId(eventExternalApiResponse.getEventId())
                .currentScore(eventExternalApiResponse.getCurrentScore())
                .timestamp(Instant.now().toEpochMilli())
                .build();

        kafkaTemplate.send(topic, message.getEventId(), message);
    }
}
