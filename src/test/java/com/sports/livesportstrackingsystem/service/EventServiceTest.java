package com.sports.livesportstrackingsystem.service;

import com.sports.livesportstrackingsystem.model.Event;
import com.sports.livesportstrackingsystem.model.EventStatus;
import com.sports.livesportstrackingsystem.model.EventStatusRequest;
import com.sports.livesportstrackingsystem.scheduler.EventScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventServiceTest {

    @Mock
    private EventScheduler eventScheduler; // Mock the dependency

    private EventService eventService; // The service under test

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
        eventService = new EventService(eventScheduler); // Inject mock into service
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_NewEvent() {
        // Arrange
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event123");
        request.setStatus(EventStatus.LIVE);

        // Act
        eventService.updateStatusEvent(request);

        // Assert
        // Verify event is added and status is LIVE
        Optional<Event> event = eventService.getEvent("event123");
        assertTrue(event.isPresent(), "Event should be present after going LIVE");
        assertEquals(EventStatus.LIVE, event.get().getStatus(), "Event status should be LIVE");

        // Verify scheduler was started once for a new LIVE event
        verify(eventScheduler, times(1)).startScheduler("event123");
        verifyNoMoreInteractions(eventScheduler); // No other scheduler interactions
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_AlreadyLiveUpdatesTimestamp() throws InterruptedException {
        // Arrange
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId("event123");
        liveRequest.setStatus(EventStatus.LIVE);

        // First, make the event LIVE
        eventService.updateStatusEvent(liveRequest);
        Optional<Event> initialEvent = eventService.getEvent("event123");
        assertTrue(initialEvent.isPresent());
        long initialTimestamp = initialEvent.get().getLastUpdated();

        // Simulate a small delay to ensure timestamp changes
        Thread.sleep(10); // Sleep for 10 milliseconds

        // Act: Update status again (should only update timestamp)
        eventService.updateStatusEvent(liveRequest);

        // Assert
        Optional<Event> updatedEvent = eventService.getEvent("event123");
        assertTrue(updatedEvent.isPresent(), "Event should still be present");
        assertEquals(EventStatus.LIVE, updatedEvent.get().getStatus(), "Event status should remain LIVE");
        assertTrue(updatedEvent.get().getLastUpdated() > initialTimestamp, "Timestamp should be updated");

        // Verify startScheduler was called ONLY ONCE (from the initial LIVE transition)
        verify(eventScheduler, times(1)).startScheduler("event123");
        verifyNoMoreInteractions(eventScheduler); // No other scheduler interactions after the first start
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_FromNotLive() {
        // Arrange
        EventStatusRequest notLiveRequest = new EventStatusRequest();
        notLiveRequest.setEventId("event123");
        notLiveRequest.setStatus(EventStatus.NOT_LIVE);

        // First, explicitly set it to NOT_LIVE (or ensure it's not present)
        // For clarity, let's ensure it's not present and then test the transition from implicit NOT_LIVE to LIVE
        eventService.updateStatusEvent(notLiveRequest); // This initial call does nothing if event is not LIVE

        // Arrange for actual test
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId("event123");
        liveRequest.setStatus(EventStatus.LIVE);

        // Act
        eventService.updateStatusEvent(liveRequest);

        // Assert
        Optional<Event> event = eventService.getEvent("event123");
        assertTrue(event.isPresent(), "Event should be present after transitioning to LIVE");
        assertEquals(EventStatus.LIVE, event.get().getStatus(), "Event status should be LIVE");
        verify(eventScheduler, times(1)).startScheduler("event123");
        verifyNoMoreInteractions(eventScheduler);
    }


    @Test
    void testUpdateStatusEvent_NotLiveEvent_FromLive() {
        // Arrange: Make the event LIVE first so we can transition it
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId("event123");
        liveRequest.setStatus(EventStatus.LIVE);
        eventService.updateStatusEvent(liveRequest); // This calls startScheduler once

        EventStatusRequest notLiveRequest = new EventStatusRequest();
        notLiveRequest.setEventId("event123");
        notLiveRequest.setStatus(EventStatus.NOT_LIVE);

        // Act
        eventService.updateStatusEvent(notLiveRequest);

        // Assert
        Optional<Event> event = eventService.getEvent("event123");
        assertFalse(event.isPresent(), "Event should be removed after going NOT_LIVE");

        // Verify startScheduler was called once (from initial LIVE transition) and stopScheduler once
        verify(eventScheduler, times(1)).startScheduler("event123");
        verify(eventScheduler, times(1)).stopScheduler("event123");
        verifyNoMoreInteractions(eventScheduler);
    }

    @Test
    void testUpdateStatusEvent_NotLiveEvent_AlreadyNotLiveOrNotFound_NoAction() {
        // Arrange: Event is not in the map, or already NOT_LIVE (we'll start with not in map)
        EventStatusRequest notLiveRequest = new EventStatusRequest();
        notLiveRequest.setEventId("nonExistentEvent");
        notLiveRequest.setStatus(EventStatus.NOT_LIVE);

        // Act
        eventService.updateStatusEvent(notLiveRequest);

        // Assert
        Optional<Event> event = eventService.getEvent("nonExistentEvent");
        assertFalse(event.isPresent(), "Event should not be added if it's NOT_LIVE and not found/already NOT_LIVE");

        // Verify NO interactions with the scheduler
        verifyNoInteractions(eventScheduler);
    }

    @Test
    void testGetEvent_Found() {
        // Arrange
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event456");
        request.setStatus(EventStatus.LIVE);
        eventService.updateStatusEvent(request); // Add an event to the map

        // Act
        Optional<Event> event = eventService.getEvent("event456");

        // Assert
        assertTrue(event.isPresent(), "Event should be found");
        assertEquals("event456", event.get().getEventId(), "Event ID should match");
    }

    @Test
    void testGetEvent_NotFound() {
        // Act
        Optional<Event> event = eventService.getEvent("nonexistent");

        // Assert
        assertFalse(event.isPresent(), "Event should not be found");
    }
}