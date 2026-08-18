package com.waypoint.carpool.service;

import com.waypoint.carpool.dto.ride.LocationUpdateRequest;
import com.waypoint.carpool.dto.ride.RideLocationResponse;
import com.waypoint.carpool.dto.ride.RideRequest;
import com.waypoint.carpool.dto.ride.RideResponse;
import com.waypoint.carpool.dto.ride.RideStopRequest;
import com.waypoint.carpool.dto.ride.RideUpdateRequest;
import com.waypoint.carpool.entity.Car;
import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.RideStop;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.BookingStatus;
import com.waypoint.carpool.entity.enums.NotificationType;
import com.waypoint.carpool.entity.enums.RideStatus;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.RideBookingRepository;
import com.waypoint.carpool.repository.RideRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideService {

    private final RideRepository rideRepository;
    private final RideBookingRepository rideBookingRepository;
    private final CarService carService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final BigDecimal commissionPercent;

    public RideService(
            RideRepository rideRepository,
            RideBookingRepository rideBookingRepository,
            CarService carService,
            PaymentService paymentService,
            NotificationService notificationService,
            @Value("${app.commission.percent}") BigDecimal commissionPercent
    ) {
        this.rideRepository = rideRepository;
        this.rideBookingRepository = rideBookingRepository;
        this.carService = carService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.commissionPercent = commissionPercent;
    }

//    public List<RideResponse> search(String source, String destination, LocalDate date) {
//        LocalDateTime dayStart = date != null ? LocalDateTime.of(date, LocalTime.MIN) : null;
//        LocalDateTime dayEnd = date != null ? LocalDateTime.of(date, LocalTime.MAX) : null;
//
//        // Search results never include a driver's pricing breakdown, only what riders should see.
//        return rideRepository.search(RideStatus.SCHEDULED, source == null ? "" : source, destination == null ? "" : destination, dayStart, dayEnd)
//                .stream().map(RideResponse::from).collect(Collectors.toList());
//    }
public List<RideResponse> search(String source, String destination) {

    // Only exclude rides that have already departed — don't restrict to
    // "today"; a ride departing next week should still show up in search.
    LocalDateTime dayStart = LocalDateTime.now();
    LocalDateTime dayEnd = LocalDateTime.now().plusYears(10);

    return rideRepository.search(
                    RideStatus.SCHEDULED,
                    source == null ? "" : source,
                    destination == null ? "" : destination,
                    dayStart,
                    dayEnd
            ).stream()
            .map(RideResponse::from)
            .collect(Collectors.toList());
}

    public Ride getRideOrThrow(Long id) {
        return rideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
    }

    /** @param caller used only to decide whether to include the financial breakdown (driver-only). */
    public RideResponse getById(Long id, User caller) {
        Ride ride = getRideOrThrow(id);
        boolean isDriver = caller != null && ride.getDriver().getId().equals(caller.getId());
        return RideResponse.from(ride, isDriver, isDriver ? bookedSeatsFor(ride) : 0);
    }

    public List<RideResponse> myDrivingRides(User driver) {
        return rideRepository.findByDriverOrderByDepartureTimeDesc(driver)
                .stream().map(r -> RideResponse.from(r, true, bookedSeatsFor(r))).collect(Collectors.toList());
    }

    private int bookedSeatsFor(Ride ride) {
        return rideBookingRepository.findByRide(ride).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .mapToInt(RideBooking::getSeatsBooked)
                .sum();
    }

    @Transactional
    public RideResponse createRide(User driver, RideRequest req) {
        Car car = carService.getOwnedCarOrThrow(driver, req.carId());

        if (req.availableSeats() > car.getSeats()) {
            throw new BadRequestException("Available seats can't exceed the car's seat count (" + car.getSeats() + ")");
        }

        BigDecimal totalPrice = req.totalPrice() != null ? req.totalPrice() : BigDecimal.ZERO;

        Ride ride = new Ride();
        ride.setDriver(driver);
        ride.setCar(car);
        ride.setSource(req.source());
        ride.setDestination(req.destination());
        ride.setSourceLat(req.sourceLat());
        ride.setSourceLng(req.sourceLng());
        ride.setDestinationLat(req.destinationLat());
        ride.setDestinationLng(req.destinationLng());
        ride.setDepartureTime(req.departureTime());
        ride.setAvailableSeats(req.availableSeats());
        ride.setPricePerSeat(req.pricePerSeat() != null ? req.pricePerSeat() : BigDecimal.ZERO);
        ride.setNotes(req.notes());
        ride.setTotalPrice(totalPrice);
        applyCommission(ride, totalPrice);

        if (req.stops() != null) {
            int order = 1;
            for (RideStopRequest sr : req.stops()) {
                if (sr.stopName() == null || sr.stopName().isBlank()) continue;
                RideStop stop = new RideStop();
                stop.setRide(ride);
                stop.setStopName(sr.stopName());
                stop.setStopOrder(order++);
                stop.setEstimatedTime(sr.estimatedTime());
                stop.setLat(sr.lat());
                stop.setLng(sr.lng());
                ride.getStops().add(stop);
            }
        }

        ride = rideRepository.save(ride);
        return RideResponse.from(ride, true); // driver sees their own financial breakdown right away
    }

    /**
     * Lets the driver edit a ride's details before it's started. Riders can
     * only ever count on details for a ride that's still SCHEDULED — once
     * it's ONGOING/COMPLETED/CANCELLED, editing is refused. "availableSeats"
     * in the request is the *total* seats offered (same meaning as at
     * creation), so we translate it into remaining open seats by
     * subtracting whatever's already booked.
     */
    @Transactional
    public RideResponse updateRide(User driver, Long rideId, RideUpdateRequest req) {
        Ride ride = getRideOrThrow(rideId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("Only the driver can edit this ride");
        }
        if (ride.getStatus() != RideStatus.SCHEDULED) {
            throw new BadRequestException("This ride can no longer be edited — it has already started, finished, or been cancelled");
        }

        Car car = carService.getOwnedCarOrThrow(driver, req.carId());

        int bookedSeats = bookedSeatsFor(ride);

        if (req.availableSeats() > car.getSeats()) {
            throw new BadRequestException("Available seats can't exceed the car's seat count (" + car.getSeats() + ")");
        }
        if (req.availableSeats() < bookedSeats) {
            throw new BadRequestException("Can't set seats below " + bookedSeats + " — that many are already booked");
        }

        BigDecimal totalPrice = req.totalPrice() != null ? req.totalPrice() : BigDecimal.ZERO;

        ride.setCar(car);
        ride.setSource(req.source());
        ride.setDestination(req.destination());
        ride.setSourceLat(req.sourceLat());
        ride.setSourceLng(req.sourceLng());
        ride.setDestinationLat(req.destinationLat());
        ride.setDestinationLng(req.destinationLng());
        ride.setDepartureTime(req.departureTime());
        ride.setAvailableSeats(req.availableSeats() - bookedSeats);
        ride.setPricePerSeat(req.pricePerSeat() != null ? req.pricePerSeat() : BigDecimal.ZERO);
        ride.setNotes(req.notes());
        ride.setTotalPrice(totalPrice);
        applyCommission(ride, totalPrice);

        ride.getStops().clear();
        if (req.stops() != null) {
            int order = 1;
            for (RideStopRequest sr : req.stops()) {
                if (sr.stopName() == null || sr.stopName().isBlank()) continue;
                RideStop stop = new RideStop();
                stop.setRide(ride);
                stop.setStopName(sr.stopName());
                stop.setStopOrder(order++);
                stop.setEstimatedTime(sr.estimatedTime());
                stop.setLat(sr.lat());
                stop.setLng(sr.lng());
                ride.getStops().add(stop);
            }
        }

        ride = rideRepository.save(ride);

        if (bookedSeats > 0) {
            notifyRidersOfEdit(ride);
        }

        return RideResponse.from(ride, true, bookedSeats);
    }

    private void notifyRidersOfEdit(Ride ride) {
        List<RideBooking> affected = rideBookingRepository.findByRide(ride).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
        for (RideBooking booking : affected) {
            notificationService.notify(
                    booking.getRider(),
                    NotificationType.RIDE_UPDATED,
                    "Ride details updated",
                    "The driver updated the ride from " + ride.getSource() + " to " + ride.getDestination()
                            + " — check the departure time, route, and price.",
                    ride.getId(),
                    booking.getId()
            );
        }
    }

    @Transactional
    public RideResponse updateStatus(User driver, Long rideId, RideStatus newStatus) {
        Ride ride = getRideOrThrow(rideId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("Only the driver can update this ride's status");
        }

        assertValidTransition(ride.getStatus(), newStatus);

        ride.setStatus(newStatus);
        ride = rideRepository.save(ride);

        if (newStatus == RideStatus.CANCELLED) {
            // The whole ride is off — every rider who requested or was
            // confirmed needs their booking cancelled and to be told why,
            // not left thinking they still have a seat.
            cancelAllBookingsForCancelledRide(ride);
        } else if (newStatus == RideStatus.ONGOING) {
            // Any join request the driver never acted on is moot once the
            // ride is underway — auto-decline it and free the held seat
            // rather than leaving it stuck in PENDING forever.
            declineStalePendingRequests(ride);
        }

        // Payment is only requested once the ride has actually happened —
        // safer for riders than paying up-front for a ride that might get cancelled.
        if (newStatus == RideStatus.COMPLETED) {
            requestPaymentsForConfirmedRiders(ride);
        }

        return RideResponse.from(ride, true);
    }

    /** SCHEDULED -> ONGOING -> COMPLETED, or SCHEDULED -> CANCELLED. No other jumps. */
    private void assertValidTransition(RideStatus from, RideStatus to) {
        boolean valid = switch (from) {
            case SCHEDULED -> to == RideStatus.ONGOING || to == RideStatus.CANCELLED;
            case ONGOING -> to == RideStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BadRequestException("Can't change ride status from " + from + " to " + to);
        }
    }

    private void cancelAllBookingsForCancelledRide(Ride ride) {
        List<RideBooking> toCancel = rideBookingRepository.findByRide(ride).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
        for (RideBooking booking : toCancel) {
            booking.setStatus(BookingStatus.CANCELLED);
            rideBookingRepository.save(booking);
            notificationService.notify(
                    booking.getRider(),
                    NotificationType.RIDE_CANCELLED,
                    "Ride cancelled",
                    "The driver cancelled the ride from " + ride.getSource() + " to " + ride.getDestination() + ".",
                    ride.getId(),
                    booking.getId()
            );
        }
    }

    private void declineStalePendingRequests(Ride ride) {
        List<RideBooking> stalePending = rideBookingRepository.findByRide(ride).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .collect(Collectors.toList());
        for (RideBooking booking : stalePending) {
            booking.setStatus(BookingStatus.CANCELLED);
            rideBookingRepository.save(booking);
            adjustAvailableSeats(ride, booking.getSeatsBooked());
            notificationService.notify(
                    booking.getRider(),
                    NotificationType.BOOKING_DECLINED,
                    "Booking declined",
                    "The ride from " + ride.getSource() + " to " + ride.getDestination()
                            + " started before the driver responded to your request.",
                    ride.getId(),
                    booking.getId()
            );
        }
    }

    /**
     * Driver pushes their current GPS position while the ride is in progress.
     * Only the ride's driver may call this, and only once the ride has
     * actually started — no point tracking a ride that hasn't begun.
     */
    @Transactional
    public RideLocationResponse updateDriverLocation(User driver, Long rideId, LocationUpdateRequest req) {
        Ride ride = getRideOrThrow(rideId);
        if (!ride.getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("Only the driver can update this ride's location");
        }
        if (ride.getStatus() != RideStatus.ONGOING) {
            throw new BadRequestException("Location can only be updated while the ride is ongoing");
        }
        ride.setCurrentLat(req.lat());
        ride.setCurrentLng(req.lng());
        ride.setLocationUpdatedAt(Instant.now());
        ride = rideRepository.save(ride);
        return RideLocationResponse.from(ride);
    }

    /**
     * Polled by riders (and the driver) to read the ride's live location.
     * Restricted to the driver themselves or someone who has booked a seat,
     * so a driver's whereabouts aren't broadcast to arbitrary users.
     */
    public RideLocationResponse getDriverLocation(User caller, Long rideId) {
        Ride ride = getRideOrThrow(rideId);
        boolean isDriver = ride.getDriver().getId().equals(caller.getId());
        boolean hasBooking = rideBookingRepository.findByRideAndRider(ride, caller).isPresent();
        if (!isDriver && !hasBooking) {
            throw new ForbiddenException("You don't have access to this ride's location");
        }
        return RideLocationResponse.from(ride);
    }

    private void requestPaymentsForConfirmedRiders(Ride ride) {
        List<RideBooking> confirmed = rideBookingRepository.findByRide(ride).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
        for (RideBooking booking : confirmed) {
            paymentService.ensurePaymentForCompletedRide(booking);
        }
    }

    @Transactional
    public void adjustAvailableSeats(Ride ride, int delta) {
        int updated = ride.getAvailableSeats() + delta;
        if (updated < 0) {
            throw new BadRequestException("Not enough seats available");
        }
        ride.setAvailableSeats(updated);
        rideRepository.save(ride);
    }

    @Transactional
    public void updateTotalRide(Ride ride) {
        BigDecimal updated = ride.getPricePerSeat()
                .add(ride.getTotalPrice());

        ride.setTotalPrice(updated);
        rideRepository.save(ride);
    }

    /**
     * Computes the platform's cut of a ride's total price at creation time
     * and stamps both the rate and the amount onto the ride, so it stays
     * accurate even if the global commission rate changes later. This is
     * bookkeeping only — no actual charge/payout is processed.
     */
    private void applyCommission(Ride ride, BigDecimal totalPrice) {
        BigDecimal amount = totalPrice
                .multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        ride.setPlatformCommissionPercent(commissionPercent);
        ride.setPlatformCommissionAmount(amount);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
