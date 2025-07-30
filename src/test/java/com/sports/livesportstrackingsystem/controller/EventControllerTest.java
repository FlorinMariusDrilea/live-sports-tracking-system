package com.sports.livesportstrackingsystem.controller;

import com.sports.livesportstrackingsystem.model.EventStatus;
import com.sports.livesportstrackingsystem.model.EventStatusRequest;
import com.sports.livesportstrackingsystem.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EventControllerTest {

    @Mock
    private EventService eventService;

    private EventController eventController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventController = new EventController(eventService);
    }

    @Test
    void testUpdateEventStatus_ValidRequest() {
        // Arrange
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event123");
        request.setStatus(EventStatus.LIVE);

        // Act
        ResponseEntity<String> response = eventController.updateEventStatus(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Event status updated successfully for event: event123", response.getBody());
        verify(eventService, times(1)).updateStatusEvent(request);
    }

    @Test
    void testUpdateEventStatus_ServiceThrowsException() {
        // Arrange
        EventStatusRequest request = new EventStatusRequest();
        request.setEventId("event123");
        request.setStatus(EventStatus.LIVE);

        doThrow(new RuntimeException("Service error")).when(eventService).updateStatusEvent(request);

        // Act
        ResponseEntity<String> response = eventController.updateEventStatus(request);

        // Assert
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Failed to update event status: Service error", response.getBody());
        verify(eventService, times(1)).updateStatusEvent(request);
    }
}