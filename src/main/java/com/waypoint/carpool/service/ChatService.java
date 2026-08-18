package com.waypoint.carpool.service;

import com.waypoint.carpool.dto.chat.ChatMessageRequest;
import com.waypoint.carpool.dto.chat.ChatMessageResponse;
import com.waypoint.carpool.entity.ChatMessage;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.BookingStatus;
import com.waypoint.carpool.entity.enums.NotificationType;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.ChatMessageRepository;
import com.waypoint.carpool.repository.RideBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One chat thread per booking, between that booking's rider and the ride's
 * driver. Riders and drivers can message about a specific ride/booking
 * (coordinating pickup, running late, etc.) without sharing phone numbers.
 */
@Service
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RideBookingRepository bookingRepository;
    private final NotificationService notificationService;

    public ChatService(
            ChatMessageRepository chatMessageRepository,
            RideBookingRepository bookingRepository,
            NotificationService notificationService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    private RideBooking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    /** Either participant: the rider who made the booking, or the ride's driver. */
    private void assertParticipant(User caller, RideBooking booking) {
        boolean isRider = booking.getRider().getId().equals(caller.getId());
        boolean isDriver = booking.getRide().getDriver().getId().equals(caller.getId());
        if (!isRider && !isDriver) {
            throw new ForbiddenException("You don't have access to this chat");
        }
    }

    private User otherParticipant(User caller, RideBooking booking) {
        boolean callerIsRider = booking.getRider().getId().equals(caller.getId());
        return callerIsRider ? booking.getRide().getDriver() : booking.getRider();
    }

    /**
     * Loads the thread and marks every message from the other participant
     * as read (the caller is, by definition, reading them right now).
     */
    @Transactional
    public List<ChatMessageResponse> getMessages(User caller, Long bookingId) {
        RideBooking booking = getBookingOrThrow(bookingId);
        assertParticipant(caller, booking);

        List<ChatMessage> messages = chatMessageRepository.findByBookingOrderByCreatedAtAsc(booking);

        List<ChatMessage> unreadFromOther = chatMessageRepository
                .findByBookingAndSenderNotAndReadAtIsNull(booking, caller);
        if (!unreadFromOther.isEmpty()) {
            Instant now = Instant.now();
            unreadFromOther.forEach(m -> m.setReadAt(now));
            chatMessageRepository.saveAll(unreadFromOther);
        }

        return messages.stream().map(m -> ChatMessageResponse.from(m, caller)).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse sendMessage(User sender, Long bookingId, ChatMessageRequest req) {
        RideBooking booking = getBookingOrThrow(bookingId);
        assertParticipant(sender, booking);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("This booking was cancelled — messaging is closed");
        }

        ChatMessage message = new ChatMessage();
        message.setBooking(booking);
        message.setSender(sender);
        message.setContent(req.content().trim());
        message = chatMessageRepository.save(message);

        User recipient = otherParticipant(sender, booking);
        String route = booking.getRide().getSource() + " to " + booking.getRide().getDestination();
        notificationService.notify(
                recipient,
                NotificationType.MESSAGE_RECEIVED,
                "New message from " + sender.getFullName(),
                message.getContent().length() > 120 ? message.getContent().substring(0, 117) + "..." : message.getContent(),
                booking.getRide().getId(),
                booking.getId()
        );

        return ChatMessageResponse.from(message, sender);
    }

    public long unreadCount(User user) {
        return chatMessageRepository.countUnreadForUser(user);
    }
}
