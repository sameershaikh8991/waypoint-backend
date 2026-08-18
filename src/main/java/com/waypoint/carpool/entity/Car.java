package com.waypoint.carpool.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    private String color;

    @Column(nullable = false)
    private String plateNumber;

    @Column(nullable = false)
    private int seats;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Car() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public Instant getCreatedAt() { return createdAt; }
}
