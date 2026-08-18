package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.ChatMessage;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByBookingOrderByCreatedAtAsc(RideBooking booking);

    List<ChatMessage> findByBookingAndSenderNotAndReadAtIsNull(RideBooking booking, User notSender);

    // Global unread count across every thread this user is part of (as
    // either the rider on the booking or the driver on the booking's ride),
    // excluding messages they sent themselves.
    @Query("""
        SELECT COUNT(m) FROM ChatMessage m
        WHERE m.readAt IS NULL
          AND m.sender <> :user
          AND (m.booking.rider = :user OR m.booking.ride.driver = :user)
        """)
    long countUnreadForUser(@Param("user") User user);
}
