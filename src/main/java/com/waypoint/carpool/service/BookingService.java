package com.waypoint.carpool.service;

import com.waypoint.carpool.dto.booking.BookingRequest;
import com.waypoint.carpool.dto.booking.BookingResponse;
import com.waypoint.carpool.dto.booking.BookingWithRideResponse;
import com.waypoint.carpool.dto.payment.MarkPaidRequest;
import com.waypoint.carpool.dto.payment.PaymentResponse;
import com.waypoint.carpool.dto.payment.RazorpayOrderResponse;
import com.waypoint.carpool.dto.payment.RazorpayVerifyRequest;
import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.RideStop;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.BookingStatus;
import com.waypoint.carpool.entity.enums.NotificationType;
import com.waypoint.carpool.entity.enums.PaymentStatus;
import com.waypoint.carpool.entity.enums.RideStatus;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.RideBookingRepository;
import com.waypoint.carpool.repository.RideStopRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingService {

    private final RideBookingRepository bookingRepository;
    private final RideStopRepository rideStopRepository;
    private final RideService rideService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;

    public BookingService(
            RideBookingRepository bookingRepository,
            RideStopRepository rideStopRepository,
            RideService rideService,
            NotificationService notificationService,
            PaymentService paymentService
    ) {
        this.bookingRepository = bookingRepository;
        this.rideStopRepository = rideStopRepository;
        this.rideService = rideService;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
    }

    @Transactional
    public BookingResponse createBooking(User rider, Long rideId, BookingRequest req) {

        Ride ride = rideService.getRideOrThrow(rideId);

        log.info("booking ride : {}",ride);

        if (ride.getDriver().getId().equals(rider.getId())) {
            throw new BadRequestException("You can't join your own ride");
        }
        if (ride.getStatus() != RideStatus.SCHEDULED) {
            throw new BadRequestException("This ride is no longer accepting riders");
        }
        if (bookingRepository.findByRideAndRider(ride, rider).isPresent()) {
            throw new BadRequestException("You've already requested to join this ride");
        }
        if (req.seatsBooked() > ride.getAvailableSeats()) {
            throw new BadRequestException("Only " + ride.getAvailableSeats() + " seat(s) left");
        }

        RideStop pickupStop = null;
        if (req.pickupStopId() != null) {
            pickupStop = rideStopRepository.findById(req.pickupStopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pickup stop not found"));
            if (!pickupStop.getRide().getId().equals(ride.getId())) {
                throw new BadRequestException("That stop doesn't belong to this ride");
            }
        }

        RideBooking booking = new RideBooking();
        booking.setRide(ride);
        booking.setRider(rider);
        booking.setPickupStop(pickupStop);
        booking.setSeatsBooked(req.seatsBooked());
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);

        rideService.adjustAvailableSeats(ride, -req.seatsBooked());
        rideService.updateTotalRide(ride);

        notificationService.notify(
                ride.getDriver(),
                NotificationType.BOOKING_REQUESTED,
                "New ride request",
                rider.getFullName() + " wants to join your ride from " + ride.getSource() + " to " + ride.getDestination() + ".",
                ride.getId(),
                booking.getId()
        );

        return BookingResponse.from(booking);
    }

    public List<BookingResponse> ridersForRide(User caller, Long rideId) {
        Ride ride = rideService.getRideOrThrow(rideId);
        if (!ride.getDriver().getId().equals(caller.getId())) {
            throw new ForbiddenException("Only the driver can view this ride's bookings");
        }
        return bookingRepository.findByRide(ride).stream()
                .map(b -> BookingResponse.from(b, paymentService.statusForBooking(b)))
                .collect(Collectors.toList());
    }

    public Optional<BookingResponse> myBookingForRide(User rider, Long rideId) {
        Ride ride = rideService.getRideOrThrow(rideId);
        return bookingRepository.findByRideAndRider(ride, rider)
                .map(b -> BookingResponse.from(b, paymentService.statusForBooking(b)));
    }

    public List<BookingWithRideResponse> myRidingHistory(User rider) {
        return bookingRepository.findByRiderOrderByCreatedAtDesc(rider)
                .stream().map(BookingWithRideResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updateStatus(User caller, Long bookingId, BookingStatus newStatus) {
        RideBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Ride ride = booking.getRide();
        boolean isDriver = ride.getDriver().getId().equals(caller.getId());
        boolean isRider = booking.getRider().getId().equals(caller.getId());

        if (!isDriver && !isRider) {
            throw new ForbiddenException("You don't have access to this booking");
        }

        if (isRider && !isDriver && newStatus != BookingStatus.CANCELLED) {
            throw new ForbiddenException("Riders can only cancel their own booking");
        }

        if (isDriver && newStatus == BookingStatus.CANCELLED && booking.getStatus() == BookingStatus.CANCELLED) {
            return BookingResponse.from(booking); // no-op
        }

        // Once the ride has actually happened (and especially once the driver
        // has confirmed/received payment for it), neither side can cancel the
        // booking anymore — there's nothing left to cancel.
        if (newStatus == BookingStatus.CANCELLED) {
            if (ride.getStatus() == RideStatus.COMPLETED) {
                throw new BadRequestException("This ride has already been completed — the booking can no longer be cancelled");
            }
            PaymentStatus paymentStatus = paymentService.statusForBooking(booking);
            if (paymentStatus == PaymentStatus.PAID) {
                throw new BadRequestException("Payment has already been received for this booking — it can no longer be cancelled");
            }
        }

        BookingStatus previous = booking.getStatus();
        booking.setStatus(newStatus);
        booking = bookingRepository.save(booking);

        // Give the seats back to the ride whenever a pending/confirmed booking becomes cancelled
        boolean wasHoldingSeats = previous == BookingStatus.PENDING || previous == BookingStatus.CONFIRMED;
        if (wasHoldingSeats && newStatus == BookingStatus.CANCELLED) {
            rideService.adjustAvailableSeats(ride, booking.getSeatsBooked());
        }

        notifyStatusChange(booking, ride, isDriver, previous, newStatus);

        return BookingResponse.from(booking);
    }

    private RideBooking getBookingOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    public PaymentResponse getPayment(User caller, Long bookingId) {
        RideBooking booking = getBookingOrThrow(bookingId);
        return paymentService.getForBooking(caller, bookingId, booking);
    }

    public PaymentResponse markPaymentPaid(User rider, Long bookingId, MarkPaidRequest req) {
        RideBooking booking = getBookingOrThrow(bookingId);
        return paymentService.markPaid(rider, booking, req.transactionRef());
    }

    /** Driver-side confirmation that a rider's payment came through. */
    public PaymentResponse confirmPaymentReceived(User driver, Long bookingId, MarkPaidRequest req) {
        RideBooking booking = getBookingOrThrow(bookingId);
        return paymentService.confirmReceivedByDriver(driver, booking, req.transactionRef());
    }

    public RazorpayOrderResponse createRazorpayOrder(User rider, Long bookingId) {
        RideBooking booking = getBookingOrThrow(bookingId);
        return paymentService.createRazorpayOrder(rider, booking);
    }

    public PaymentResponse verifyRazorpayPayment(User rider, Long bookingId, RazorpayVerifyRequest req) {
        RideBooking booking = getBookingOrThrow(bookingId);
        return paymentService.verifyRazorpayPayment(rider, booking, req);
    }

    private void notifyStatusChange(RideBooking booking, Ride ride, boolean isDriver, BookingStatus previous, BookingStatus newStatus) {
        String route = ride.getSource() + " to " + ride.getDestination();

        if (isDriver && previous == BookingStatus.PENDING && newStatus == BookingStatus.CONFIRMED) {
            notificationService.notify(
                    booking.getRider(),
                    NotificationType.BOOKING_CONFIRMED,
                    "Booking confirmed",
                    "Your seat on the ride from " + route + " was confirmed by the driver.",
                    ride.getId(),
                    booking.getId()
            );
        } else if (isDriver && previous == BookingStatus.PENDING && newStatus == BookingStatus.CANCELLED) {
            notificationService.notify(
                    booking.getRider(),
                    NotificationType.BOOKING_DECLINED,
                    "Booking declined",
                    "The driver declined your request to join the ride from " + route + ".",
                    ride.getId(),
                    booking.getId()
            );
        } else if (!isDriver && previous == BookingStatus.CONFIRMED && newStatus == BookingStatus.CANCELLED) {
            notificationService.notify(
                    ride.getDriver(),
                    NotificationType.BOOKING_CANCELLED,
                    "Rider cancelled",
                    booking.getRider().getFullName() + " cancelled their confirmed seat on your ride from " + route + ".",
                    ride.getId(),
                    booking.getId()
            );
        }
    }
}
