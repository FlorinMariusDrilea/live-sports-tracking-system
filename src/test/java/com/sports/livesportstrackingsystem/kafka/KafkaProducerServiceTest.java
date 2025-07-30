package com.sports.livesportstrackingsystem.kafka;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import com.sports.livesportstrackingsystem.model.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, EventMessage> kafkaTemplate;

    private KafkaProducerService kafkaProducerService;

    private static final String TEST_TOPIC = "event-updates-topic";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kafkaProducerService = new KafkaProducerService(kafkaTemplate);

        // Manually set the @Value field for testing, as Spring doesn't inject it in unit tests
        // You might need to use reflection or a test-specific constructor if you can't add a setter
        // For simplicity, let's assume you can set it or the service allows it for testing
        // (If not, you'd usually use @TestPropertySource or SpringBootTest for integration tests)
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
    void testPublishSuccess() {
        EventExternalApiResponse eventExternalApiResponse = new EventExternalApiResponse();
        eventExternalApiResponse.setEventId("event123");
        eventExternalApiResponse.setCurrentScore("2-1");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventMessage> messageCaptor = ArgumentCaptor.forClass(EventMessage.class);

        // Mock a successful send result
        SendResult<String, EventMessage> mockSendResult = mock(SendResult.class); // Mock the SendResult
        CompletableFuture<SendResult<String, EventMessage>> completedFuture = CompletableFuture.completedFuture(mockSendResult);

        long startTime = Instant.now().toEpochMilli();

        when(kafkaTemplate.send(anyString(), anyString(), any(EventMessage.class)))
                .thenReturn(completedFuture);

        // Act
        kafkaProducerService.publish(eventExternalApiResponse);

        // Assert
        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), messageCaptor.capture());

        // Assert Topic
        assertEquals(TEST_TOPIC, topicCaptor.getValue(), "Kafka topic should match configured value");

        // Assert Key
        assertEquals("event123", keyCaptor.getValue(), "Kafka key should be the eventId");

        // Assert Message content
        EventMessage capturedMessage = messageCaptor.getValue();
        assertEquals("2-1", capturedMessage.getCurrentScore(), "Captured message score should match");
        assertEquals("event123", capturedMessage.getEventId(), "Captured message eventId should match");

        long endTime = Instant.now().toEpochMilli();
        assertTrue(capturedMessage.getTimestamp() >= startTime, "Timestamp should be after test start");
        assertTrue(capturedMessage.getTimestamp() <= endTime + 100, "Timestamp should be around current time (+100ms buffer)"); // Small buffer for execution time
    }

    @Test
    void testPublishFailure() {
        EventExternalApiResponse eventExternalApiResponse = new EventExternalApiResponse();
        eventExternalApiResponse.setEventId("event456");
        eventExternalApiResponse.setCurrentScore("1-0");

        CompletableFuture<SendResult<String, EventMessage>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection lost"));

        when(kafkaTemplate.send(anyString(), anyString(), any(EventMessage.class)))
                .thenReturn(failedFuture);

        // Act
        kafkaProducerService.publish(eventExternalApiResponse);

        // Assert: Verify send was attempted
        verify(kafkaTemplate, times(1)).send(anyString(), eq("event456"), any(EventMessage.class));
    }
}