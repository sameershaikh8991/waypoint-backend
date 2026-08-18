package com.waypoint.carpool.dto.notification;

import com.waypoint.carpool.entity.Notification;
import com.waypoint.carpool.entity.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long rideId,
        Long bookingId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getRideId(), n.getBookingId(), n.isRead(), n.getCreatedAt()
        );
    }
}
