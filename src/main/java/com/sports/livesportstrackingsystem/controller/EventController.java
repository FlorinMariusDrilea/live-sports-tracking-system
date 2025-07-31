package com.sports.livesportstrackingsystem.controller;

import com.sports.livesportstrackingsystem.model.EventStatusRequest;
import com.sports.livesportstrackingsystem.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);
    private final EventService eventService;
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/status")
    public ResponseEntity<String> updateEventStatus(@Valid @RequestBody EventStatusRequest request) {
        try {
            eventService.updateStatusEvent(request);
            return ResponseEntity.ok("Event status updated successfully for event: " + request.getEventId());
        } catch (Exception e) {
            logger.error("Error updating event status for eventId: {}", request.getEventId(), e);
            return ResponseEntity.internalServerError().body("Failed to update event status: " + e.getMessage());
        }
    }
}
