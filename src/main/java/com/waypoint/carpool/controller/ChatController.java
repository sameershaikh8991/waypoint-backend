package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.chat.ChatMessageRequest;
import com.waypoint.carpool.dto.chat.ChatMessageResponse;
import com.waypoint.carpool.dto.chat.UnreadChatCountResponse;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/api/bookings/{id}/messages")
    public List<ChatMessageResponse> getMessages(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return chatService.getMessages(user, id);
    }

    @PostMapping("/api/bookings/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ChatMessageRequest req
    ) {
        return chatService.sendMessage(user, id, req);
    }

    @GetMapping("/api/messages/unread-count")
    public UnreadChatCountResponse unreadCount(@AuthenticationPrincipal User user) {
        return new UnreadChatCountResponse(chatService.unreadCount(user));
    }
}
