package com.waypoint.carpool.dto.booking;

import com.waypoint.carpool.dto.ride.RideResponse;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.enums.BookingStatus;

public record BookingWithRideResponse(
        Long bookingId,
        BookingStatus status,
        int seatsBooked,
        RideResponse ride
) {
    public static BookingWithRideResponse from(RideBooking b) {
        return new BookingWithRideResponse(
                b.getId(),
                b.getStatus(),
                b.getSeatsBooked(),
                RideResponse.from(b.getRide())
        );
    }
}
