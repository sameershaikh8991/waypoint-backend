package com.waypoint.carpool.entity.enums;

public enum NotificationType {
    BOOKING_REQUESTED,   // rider -> driver: someone wants to join
    BOOKING_CONFIRMED,   // driver -> rider: request accepted
    BOOKING_DECLINED,    // driver -> rider: request declined
    BOOKING_CANCELLED,   // rider -> driver: rider cancelled a confirmed seat
    RIDE_CANCELLED,      // driver -> rider(s): the driver cancelled the whole ride
    RIDE_UPDATED,        // driver -> rider(s): the driver edited ride details before it started
    PAYMENT_RECEIVED,    // rider -> driver: rider marked their payment as paid
    PAYMENT_REQUESTED,   // driver -> rider: ride completed, payment is now due
    MESSAGE_RECEIVED     // rider <-> driver: new chat message on a booking
}
