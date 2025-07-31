package com.sports.livesportstrackingsystem.service;

import com.sports.livesportstrackingsystem.external.ExternalApiService;
import com.sports.livesportstrackingsystem.kafka.KafkaProducerService;
import com.sports.livesportstrackingsystem.model.Event;
import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import com.sports.livesportstrackingsystem.model.EventStatus;
import com.sports.livesportstrackingsystem.model.EventStatusRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class EventServiceIntegrationTest {
    @Autowired
    private EventService eventService;
    @MockBean
    private ExternalApiService externalApiService;
    @MockBean
    private KafkaProducerService kafkaProducerService;

    private ScheduledExecutorService scheduledExecutorService;

    @BeforeEach
    void setUp() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void shutDown() {
        scheduledExecutorService.shutdown();
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_StartsPollingAndPublishesToKafka() {
        String eventId = "football-match-1";
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId(eventId);
        liveRequest.setStatus(EventStatus.LIVE);

        // Mock external API response for one poll cycle
        EventExternalApiResponse mockResponse = new EventExternalApiResponse();
        mockResponse.setEventId(eventId);
        mockResponse.setCurrentScore("1-0");
        when(externalApiService.getEventScore(eventId)).thenReturn(Mono.just(mockResponse));

        // Act
        eventService.updateStatusEvent(liveRequest);

        // Assert initial state and wait for a single poll and publish
        Optional<Event> event = eventService.getEvent(eventId);
        assertTrue(event.isPresent(), "Event should be present and LIVE");
        assertEquals(EventStatus.LIVE, event.get().getStatus());

        // Mockito's timeout is good for async verification
        verify(externalApiService, timeout(1500)).getEventScore(eventId); // Wait up to 1.5s
        verify(kafkaProducerService, timeout(1500)).sendMessage(any(EventExternalApiResponse.class));

        // Capture Kafka message content more concisely
        ArgumentCaptor<EventExternalApiResponse> kafkaMessageCaptor = ArgumentCaptor.forClass(EventExternalApiResponse.class);
        verify(kafkaProducerService, atLeastOnce()).sendMessage(kafkaMessageCaptor.capture());
        assertEquals(eventId, kafkaMessageCaptor.getValue().getEventId());
        assertNotNull(kafkaMessageCaptor.getValue().getCurrentScore());
    }

    @Test
    void testUpdateStatusEvent_NotLiveEvent_StopsPolling() {
        String eventId = "handball-match-2";
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId(eventId);
        liveRequest.setStatus(EventStatus.LIVE);

        EventStatusRequest notLiveRequest = new EventStatusRequest();
        notLiveRequest.setEventId(eventId);
        notLiveRequest.setStatus(EventStatus.NOT_LIVE);

        // Mock external API to return something (needs to be called when LIVE)
        when(externalApiService.getEventScore(eventId)).thenReturn(Mono.just(new EventExternalApiResponse()));

        // Act - Start polling for the LIVE event
        eventService.updateStatusEvent(liveRequest);

        // Ensure at least one poll and Kafka message send happen before resetting mocks.
        verify(externalApiService, timeout(1500)).getEventScore(eventId);
        verify(kafkaProducerService, timeout(1500)).sendMessage(any());

        reset(externalApiService);
        reset(kafkaProducerService);

        // Act - Now, set the event to NOT_LIVE, which should stop the scheduler for this event
        eventService.updateStatusEvent(notLiveRequest);

        // Assert that the event is removed from the service's tracking
        Optional<Event> event = eventService.getEvent(eventId);
        assertFalse(event.isPresent(), "Event should be removed after going NOT_LIVE");

        // Wait 1.5 seconds
        verify(externalApiService, after(1500).never()).getEventScore(eventId);
        verify(kafkaProducerService, after(1500).never()).sendMessage(any());
    }

    @Test
    void testUpdateStatusEvent_ServiceReceivesErrorFromExternalApi_NoKafkaPublish() {
        String eventId = "tennis-match-3";
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId(eventId);
        liveRequest.setStatus(EventStatus.LIVE);

        when(externalApiService.getEventScore(eventId)).thenReturn(Mono.error(new RuntimeException("API down")));

        // Act
        eventService.updateStatusEvent(liveRequest);

        // Assert
        Optional<com.sports.livesportstrackingsystem.model.Event> event = eventService.getEvent(eventId);
        assertTrue(event.isPresent(), "Event should still be present even if API errors");
        assertEquals(EventStatus.LIVE, event.get().getStatus());

        verify(externalApiService, timeout(1500).atLeastOnce()).getEventScore(eventId);

        verify(kafkaProducerService, never()).sendMessage(any());
    }

}
