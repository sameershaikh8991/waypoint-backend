package com.waypoint.carpool.dto.ride;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record RideStopRequest(
        @NotBlank String stopName,
        LocalDateTime estimatedTime,
        Double lat,
        Double lng
) {}
