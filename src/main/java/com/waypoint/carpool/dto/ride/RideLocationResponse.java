package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.enums.RideStatus;

import java.time.Instant;

public record RideLocationResponse(
        Double lat,
        Double lng,
        Instant updatedAt,
        RideStatus rideStatus
) {
    public static RideLocationResponse from(Ride r) {
        return new RideLocationResponse(r.getCurrentLat(), r.getCurrentLng(), r.getLocationUpdatedAt(), r.getStatus());
    }
}
