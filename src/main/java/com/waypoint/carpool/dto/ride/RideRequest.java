package com.waypoint.carpool.dto.ride;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RideRequest(
        @NotNull Long carId,
        @NotBlank String source,
        @NotBlank String destination,
        // Populated by the frontend's location dropdown. Optional so manual
        // free-text entries (or older clients) don't get rejected.
        Double sourceLat,
        Double sourceLng,
        Double destinationLat,
        Double destinationLng,
        @NotNull @Future(message = "Departure time must be in the future") LocalDateTime departureTime,
        @Min(1) @Max(8) int availableSeats,
        @DecimalMin(value = "0.0") BigDecimal pricePerSeat,
        @NotNull @DecimalMin(value = "0.0") BigDecimal totalPrice,
        String notes,
        @Valid List<RideStopRequest> stops
) {}
