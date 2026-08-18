package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.enums.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record RideResponse(
        Long id,
        DriverSummary driver,
        CarSummary car,
        String source,
        String destination,
        Double sourceLat,
        Double sourceLng,
        Double destinationLat,
        Double destinationLng,
        LocalDateTime departureTime,
        int availableSeats,
        BigDecimal pricePerSeat,
        RideStatus status,
        String notes,
        List<RideStopResponse> stops,
        // Live driver location — only meaningful while status == ONGOING.
        Double currentLat,
        Double currentLng,
        Instant locationUpdatedAt,
        // Only populated when the caller is this ride's driver (see RideResponse.from)
        BigDecimal totalPrice,
        BigDecimal platformCommissionPercent,
        BigDecimal platformCommissionAmount,
        BigDecimal driverPayout,
        // Seats currently held by PENDING/CONFIRMED bookings — only
        // populated for the driver, e.g. so the edit-ride form can show
        // the *total* seats offered (availableSeats + bookedSeats).
        Integer bookedSeats
) {
    /** Public view: no financial breakdown (used in search results / other people's rides). */
    public static RideResponse from(Ride r) {
        return from(r, false, 0);
    }

    /** @param includeFinancials pass true only when the caller is r's driver. */
    public static RideResponse from(Ride r, boolean includeFinancials) {
        return from(r, includeFinancials, 0);
    }

    /** @param includeFinancials pass true only when the caller is r's driver. */
    public static RideResponse from(Ride r, boolean includeFinancials, int bookedSeats) {
        BigDecimal totalPrice = null;
        BigDecimal commissionPercent = null;
        BigDecimal commissionAmount = null;
        BigDecimal payout = null;
        Integer bookedSeatsOut = null;

        if (includeFinancials) {
            totalPrice = r.getTotalPrice();
            commissionPercent = r.getPlatformCommissionPercent();
            commissionAmount = r.getPlatformCommissionAmount();
            payout = r.getTotalPrice().subtract(r.getPlatformCommissionAmount());
            bookedSeatsOut = bookedSeats;
        }

        return new RideResponse(
                r.getId(),
                DriverSummary.from(r.getDriver()),
                CarSummary.from(r.getCar()),
                r.getSource(),
                r.getDestination(),
                r.getSourceLat(),
                r.getSourceLng(),
                r.getDestinationLat(),
                r.getDestinationLng(),
                r.getDepartureTime(),
                r.getAvailableSeats(),
                r.getPricePerSeat(),
                r.getStatus(),
                r.getNotes(),
                r.getStops().stream().map(RideStopResponse::from).collect(Collectors.toList()),
                r.getCurrentLat(),
                r.getCurrentLng(),
                r.getLocationUpdatedAt(),
                totalPrice,
                commissionPercent,
                commissionAmount,
                payout,
                bookedSeatsOut
        );
    }
}
