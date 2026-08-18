package com.waypoint.carpool.dto.booking;

import jakarta.validation.constraints.Min;

public record BookingRequest(
        @Min(1) int seatsBooked,
        Long pickupStopId
) {}
