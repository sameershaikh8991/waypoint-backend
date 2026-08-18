package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.notification.NotificationResponse;
import com.waypoint.carpool.dto.notification.UnreadCountResponse;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> mine(@AuthenticationPrincipal User user) {
        return notificationService.myNotifications(user);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal User user) {
        return new UnreadCountResponse(notificationService.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        notificationService.markRead(user, id);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user);
    }
}
