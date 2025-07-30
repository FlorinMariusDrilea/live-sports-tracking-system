package com.sports.livesportstrackingsystem.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventMessage {
    private String eventId;
    private String currentScore;
    private long timestamp;
}
