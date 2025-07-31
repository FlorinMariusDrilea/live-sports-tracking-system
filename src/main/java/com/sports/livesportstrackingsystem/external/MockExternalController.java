package com.sports.livesportstrackingsystem.external;

import com.sports.livesportstrackingsystem.model.EventExternalApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/mock-event-api")
@Slf4j
public class MockExternalController {

    private final Random random = new Random();

    @GetMapping("/{eventId}")
    public ResponseEntity<EventExternalApiResponse> getEventScore(@PathVariable String eventId) {
        // Simulate some random scores
        String score = random.nextInt(10) + ":" + random.nextInt(10);
        EventExternalApiResponse response = new EventExternalApiResponse();
        response.setEventId(eventId);
        response.setCurrentScore(score);
        log.info("Mock API: Responding for eventId {} with score: {}", eventId, score);
        return ResponseEntity.ok(response);
    }
}