package com.waypoint.carpool.entity;

import com.waypoint.carpool.entity.enums.BookingStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ride_bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ride_id", "rider_id"})
})
public class RideBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_stop_id")
    private RideStop pickupStop;

    @Column(nullable = false)
    private int seatsBooked = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RideBooking() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Ride getRide() { return ride; }
    public void setRide(Ride ride) { this.ride = ride; }

    public User getRider() { return rider; }
    public void setRider(User rider) { this.rider = rider; }

    public RideStop getPickupStop() { return pickupStop; }
    public void setPickupStop(RideStop pickupStop) { this.pickupStop = pickupStop; }

    public int getSeatsBooked() { return seatsBooked; }
    public void setSeatsBooked(int seatsBooked) { this.seatsBooked = seatsBooked; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
}
