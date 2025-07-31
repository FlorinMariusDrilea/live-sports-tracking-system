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
    private EventScheduler eventScheduler;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventService = new EventService(eventScheduler);
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_NewEvent() {
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
        verifyNoMoreInteractions(eventScheduler);
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_AlreadyLiveUpdatesTimestamp() throws InterruptedException {
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId("event123");
        liveRequest.setStatus(EventStatus.LIVE);

        // Make the event LIVE
        eventService.updateStatusEvent(liveRequest);
        Optional<Event> initialEvent = eventService.getEvent("event123");
        assertTrue(initialEvent.isPresent());
        long initialTimestamp = initialEvent.get().getLastUpdated();

        // Sleep for 10 milliseconds
        Thread.sleep(10);

        // Act: Update status again
        eventService.updateStatusEvent(liveRequest);

        // Assert
        Optional<Event> updatedEvent = eventService.getEvent("event123");
        assertTrue(updatedEvent.isPresent(), "Event should still be present");
        assertEquals(EventStatus.LIVE, updatedEvent.get().getStatus(), "Event status should remain LIVE");
        assertTrue(updatedEvent.get().getLastUpdated() > initialTimestamp, "Timestamp should be updated");

        // Verify startScheduler was called just once
        verify(eventScheduler, times(1)).startScheduler("event123");
        verifyNoMoreInteractions(eventScheduler);
    }

    @Test
    void testUpdateStatusEvent_LiveEvent_FromNotLive() {
        EventStatusRequest notLiveRequest = new EventStatusRequest();
        notLiveRequest.setEventId("event123");
        notLiveRequest.setStatus(EventStatus.NOT_LIVE);

        // First, explicitly set it to NOT_LIVE
        eventService.updateStatusEvent(notLiveRequest);

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
        EventStatusRequest liveRequest = new EventStatusRequest();
        liveRequest.setEventId("event123");
        liveRequest.setStatus(EventStatus.LIVE);
        eventService.updateStatusEvent(liveRequest);

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
    void testGetEvent_Found() {
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event456");
        request.setStatus(EventStatus.LIVE);
        eventService.updateStatusEvent(request);

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
