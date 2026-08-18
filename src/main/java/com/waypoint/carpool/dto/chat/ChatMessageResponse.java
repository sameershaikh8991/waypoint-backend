package com.waypoint.carpool.dto.chat;

import com.waypoint.carpool.entity.ChatMessage;
import com.waypoint.carpool.entity.User;

import java.time.Instant;

public record ChatMessageResponse(
        Long id,
        Long bookingId,
        Long senderId,
        String senderName,
        String content,
        boolean mine,
        Instant readAt,
        Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessage m, User caller) {
        return new ChatMessageResponse(
                m.getId(),
                m.getBooking().getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getContent(),
                m.getSender().getId().equals(caller.getId()),
                m.getReadAt(),
                m.getCreatedAt()
        );
    }
}
