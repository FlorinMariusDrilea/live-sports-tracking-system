package com.sports.livesportstrackingsystem.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Event {
    private String eventId;
    private EventStatus status;
    private long lastUpdated;
}
