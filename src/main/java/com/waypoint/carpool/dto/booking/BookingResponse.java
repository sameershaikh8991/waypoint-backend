package com.waypoint.carpool.dto.booking;

import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.enums.BookingStatus;
import com.waypoint.carpool.entity.enums.PaymentStatus;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long rideId,
        Long riderId,
        String riderName,
        String riderPhone,
        Long pickupStopId,
        String pickupStopName,
        int seatsBooked,
        BookingStatus status,
        Instant createdAt,
        // Null until a payment record exists for this booking (i.e. before the
        // ride is marked completed). Lets the driver's rider list show who's
        // paid without a separate call per booking.
        PaymentStatus paymentStatus
) {
    public static BookingResponse from(RideBooking b) {
        return from(b, null);
    }

    public static BookingResponse from(RideBooking b, PaymentStatus paymentStatus) {
        return new BookingResponse(
                b.getId(),
                b.getRide().getId(),
                b.getRider().getId(),
                b.getRider().getFullName(),
                b.getRider().getPhone(),
                b.getPickupStop() != null ? b.getPickupStop().getId() : null,
                b.getPickupStop() != null ? b.getPickupStop().getStopName() : null,
                b.getSeatsBooked(),
                b.getStatus(),
                b.getCreatedAt(),
                paymentStatus
        );
    }
}
