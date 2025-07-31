package com.sports.livesportstrackingsystem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventExternalApiResponse {
    private String eventId;
    private String currentScore;
    private Long timestamp;
}
