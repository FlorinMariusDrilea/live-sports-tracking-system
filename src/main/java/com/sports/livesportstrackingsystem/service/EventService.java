package com.sports.livesportstrackingsystem.service;

import com.sports.livesportstrackingsystem.model.Event;
import com.sports.livesportstrackingsystem.model.EventStatus;
import com.sports.livesportstrackingsystem.model.EventStatusRequest;
import com.sports.livesportstrackingsystem.scheduler.EventScheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EventService {
    private final Map<String, Event> liveEvents = new ConcurrentHashMap<>();
    private final EventScheduler eventScheduler;

    public EventService(EventScheduler eventScheduler) {
        this.eventScheduler = eventScheduler;
    }
    public void updateStatusEvent(EventStatusRequest request) {
        String eventId = request.getEventId();
        EventStatus newStatus  = request.getStatus();
        log.info("Received status update for eventId: {}, newStatus: {}", eventId, newStatus);

        Event currentEvent = liveEvents.get(eventId);

        if (newStatus == EventStatus.LIVE) {
            if (currentEvent == null || currentEvent.getStatus() == EventStatus.NOT_LIVE) {
                // event transition to LIVE or new event -> LIVE
                Event newEvent = Event.builder()
                        .eventId(eventId)
                        .status(EventStatus.LIVE)
                        .lastUpdated(System.currentTimeMillis())
                        .build();

                liveEvents.put(eventId, newEvent);
                eventScheduler.startScheduler(eventId);
                log.debug("Event {} marked as LIVE and start polling.", eventId);
            } else {
                // already Live -> update timestamp
                currentEvent.setLastUpdated(System.currentTimeMillis());
                log.debug("Event {} was already LIVE. Updating timestamp.", eventId);
            }
        } else {
            if (currentEvent != null && currentEvent.getStatus() == EventStatus.LIVE) {
                // event transition to NOT_LIVE
                liveEvents.remove(eventId);
                eventScheduler.stopScheduler(eventId);
                log.debug("Event {} marked as NOT_LIVE and polling stopped.", eventId);
            } else {
                log.debug("Event {} was already NOT_LIVE or not found. No action taken.", eventId);
            }
        }
    }

    // used for testing
    public Optional<Event> getEvent(String eventId) {
        return Optional.ofNullable(liveEvents.get(eventId));
    }
}
