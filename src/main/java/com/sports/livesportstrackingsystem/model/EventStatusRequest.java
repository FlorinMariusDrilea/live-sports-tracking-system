package com.sports.livesportstrackingsystem.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventStatusRequest {
    @NotBlank
    private String eventId;
    @NotNull
    private EventStatus status;
}
