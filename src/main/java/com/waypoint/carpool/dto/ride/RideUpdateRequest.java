package com.waypoint.carpool.dto.ride;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Same shape as RideRequest — editing a ride reuses the same fields the
 * driver set at creation time. Only allowed while the ride is still
 * SCHEDULED (see RideService.updateRide).
 */
public record RideUpdateRequest(
        @NotNull Long carId,
        @NotBlank String source,
        @NotBlank String destination,
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
