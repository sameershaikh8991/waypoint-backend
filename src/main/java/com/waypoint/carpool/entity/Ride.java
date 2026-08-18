package com.waypoint.carpool.entity;

import com.waypoint.carpool.entity.enums.RideStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String destination;

    // Lat/lng captured from the dropdown location picker on the frontend.
    // Nullable so older rows created before this field existed still load fine.
    private Double sourceLat;
    private Double sourceLng;
    private Double destinationLat;
    private Double destinationLng;

    // Live driver location, updated while status == ONGOING.
    private Double currentLat;
    private Double currentLng;
    private Instant locationUpdatedAt;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private int availableSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerSeat = BigDecimal.ZERO;

    // Set by the driver: what the whole ride is worth, independent of
    // pricePerSeat (which is what an individual rider pays).
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    // Snapshot of the platform's cut, computed at ride-creation time from
    // app.commission.percent so past rides keep the rate that applied then.
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal platformCommissionPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommissionAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.SCHEDULED;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OrderBy("stopOrder ASC")
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RideStop> stops = new ArrayList<>();

    public Ride() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getDriver() { return driver; }
    public void setDriver(User driver) { this.driver = driver; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Double getSourceLat() { return sourceLat; }
    public void setSourceLat(Double sourceLat) { this.sourceLat = sourceLat; }

    public Double getSourceLng() { return sourceLng; }
    public void setSourceLng(Double sourceLng) { this.sourceLng = sourceLng; }

    public Double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(Double destinationLat) { this.destinationLat = destinationLat; }

    public Double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(Double destinationLng) { this.destinationLng = destinationLng; }

    public Double getCurrentLat() { return currentLat; }
    public void setCurrentLat(Double currentLat) { this.currentLat = currentLat; }

    public Double getCurrentLng() { return currentLng; }
    public void setCurrentLng(Double currentLng) { this.currentLng = currentLng; }

    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public BigDecimal getPricePerSeat() { return pricePerSeat; }
    public void setPricePerSeat(BigDecimal pricePerSeat) { this.pricePerSeat = pricePerSeat; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public BigDecimal getPlatformCommissionPercent() { return platformCommissionPercent; }
    public void setPlatformCommissionPercent(BigDecimal platformCommissionPercent) { this.platformCommissionPercent = platformCommissionPercent; }

    public BigDecimal getPlatformCommissionAmount() { return platformCommissionAmount; }
    public void setPlatformCommissionAmount(BigDecimal platformCommissionAmount) { this.platformCommissionAmount = platformCommissionAmount; }

    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }

    public List<RideStop> getStops() { return stops; }
    public void setStops(List<RideStop> stops) { this.stops = stops; }
}
