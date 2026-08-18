package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.RideStop;

import java.time.LocalDateTime;

public record RideStopResponse(
        Long id,
        String stopName,
        int stopOrder,
        LocalDateTime estimatedTime,
        Double lat,
        Double lng
) {
    public static RideStopResponse from(RideStop s) {
        return new RideStopResponse(s.getId(), s.getStopName(), s.getStopOrder(), s.getEstimatedTime(), s.getLat(), s.getLng());
    }
}
