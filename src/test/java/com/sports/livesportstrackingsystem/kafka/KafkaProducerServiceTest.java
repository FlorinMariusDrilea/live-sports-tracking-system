package com.sports.livesportstrackingsystem.kafka;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, EventExternalApiResponse> kafkaTemplate;

    private KafkaProducerService kafkaProducerService;

    private static final String TEST_TOPIC = "event-updates-topic";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kafkaProducerService = new KafkaProducerService(kafkaTemplate);
        try {
            Field field = KafkaProducerService.class.getDeclaredField("topic");
            field.setAccessible(true);
            field.set(kafkaProducerService, TEST_TOPIC);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle exception if field not found or not accessible
            System.err.println("Could not set topic field via reflection: " + e.getMessage());
        }
    }

    @Test
    void testSendMessageSuccess() {
        EventExternalApiResponse eventExternalApiResponse = new EventExternalApiResponse();
        eventExternalApiResponse.setEventId("event123");
        eventExternalApiResponse.setCurrentScore("2-1");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventExternalApiResponse> messageCaptor = ArgumentCaptor.forClass(EventExternalApiResponse.class);

        long startTime = Instant.now().toEpochMilli();

        // Act
        kafkaProducerService.sendMessage(eventExternalApiResponse);

        // Assert
        // Verify that kafkaTemplate.send was called exactly once with the correct arguments
        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());

        // Assert Topic
        assertEquals(TEST_TOPIC, topicCaptor.getValue(), "Kafka topic should match configured value");

        // Assert Key
        assertEquals("event123", keyCaptor.getValue(), "Kafka key should be the eventId");

        // Assert Message content
        EventExternalApiResponse capturedMessage = messageCaptor.getValue();
        assertEquals("2-1", capturedMessage.getCurrentScore(), "Captured message score should match");
        assertEquals("event123", capturedMessage.getEventId(), "Captured message eventId should match");

        // Assert Timestamp (generated within the method)
        assertTrue(capturedMessage.getTimestamp() >= startTime, "Timestamp should be after test start");
        assertTrue(capturedMessage.getTimestamp() <= Instant.now().toEpochMilli() + 100, "Timestamp should be around current time (+100ms buffer)");
    }

    @Test
    void testSendMessageFailure() {
        EventExternalApiResponse eventExternalApiResponse = new EventExternalApiResponse();
        eventExternalApiResponse.setEventId("event456");
        eventExternalApiResponse.setCurrentScore("1-0");

        doThrow(new RuntimeException("Simulated synchronous Kafka error")).when(kafkaTemplate).send(anyString(), anyString(), any(EventExternalApiResponse.class));

        // Act & Assert
        RuntimeException thrown = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            kafkaProducerService.sendMessage(eventExternalApiResponse);
        });

        assertEquals("Simulated synchronous Kafka error", thrown.getMessage());

        // Verify that send was attempted
        verify(kafkaTemplate, times(1)).send(anyString(), eq("event456"), any(EventExternalApiResponse.class));
    }
}
