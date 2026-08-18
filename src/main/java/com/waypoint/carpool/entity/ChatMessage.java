package com.waypoint.carpool.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A single message in the chat thread attached to a booking. The thread's
 * two participants are always booking.rider and booking.ride.driver — there
 * is no group chat, and no separate "conversation" entity is needed since
 * a booking already uniquely identifies the pair of people talking.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private RideBooking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 2000)
    private String content;

    // Set once the *other* participant has fetched the thread. Null = unread.
    private Instant readAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ChatMessage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RideBooking getBooking() { return booking; }
    public void setBooking(RideBooking booking) { this.booking = booking; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
}
