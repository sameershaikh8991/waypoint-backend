package com.waypoint.carpool.service;

import com.waypoint.carpool.dto.notification.NotificationResponse;
import com.waypoint.carpool.entity.Notification;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.NotificationType;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User recipient, NotificationType type, String title, String message, Long rideId, Long bookingId) {
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setRideId(rideId);
        n.setBookingId(bookingId);
        notificationRepository.save(n);
    }

    public List<NotificationResponse> myNotifications(User user) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream().map(NotificationResponse::from).collect(Collectors.toList());
    }

    public long unreadCount(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    @Transactional
    public void markRead(User user, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getRecipient().getId().equals(user.getId())) {
            throw new ForbiddenException("This notification doesn't belong to you");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(User user) {
        List<Notification> unread = notificationRepository.findByRecipientOrderByCreatedAtDesc(user)
                .stream().filter(n -> !n.isRead()).collect(Collectors.toList());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
