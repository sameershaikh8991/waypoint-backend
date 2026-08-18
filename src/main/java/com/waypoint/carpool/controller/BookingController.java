package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.booking.BookingRequest;
import com.waypoint.carpool.dto.booking.BookingResponse;
import com.waypoint.carpool.dto.booking.BookingStatusUpdateRequest;
import com.waypoint.carpool.dto.booking.BookingWithRideResponse;
import com.waypoint.carpool.dto.payment.MarkPaidRequest;
import com.waypoint.carpool.dto.payment.PaymentResponse;
import com.waypoint.carpool.dto.payment.RazorpayOrderResponse;
import com.waypoint.carpool.dto.payment.RazorpayVerifyRequest;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/rides/{rideId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse join(
            @AuthenticationPrincipal User user,
            @PathVariable Long rideId,
            @Valid @RequestBody BookingRequest req
    ) {
        return bookingService.createBooking(user, rideId, req);
    }

    @GetMapping("/api/rides/{rideId}/bookings")
    public List<BookingResponse> ridersForRide(@AuthenticationPrincipal User user, @PathVariable Long rideId) {
        return bookingService.ridersForRide(user, rideId);
    }

    @GetMapping("/api/rides/{rideId}/bookings/mine")
    public ResponseEntity<BookingResponse> myBookingForRide(@AuthenticationPrincipal User user, @PathVariable Long rideId) {
        return bookingService.myBookingForRide(user, rideId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/bookings/mine")
    public List<BookingWithRideResponse> myRidingHistory(@AuthenticationPrincipal User user) {
        return bookingService.myRidingHistory(user);
    }

    @PatchMapping("/api/bookings/{id}/status")
    public BookingResponse updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusUpdateRequest req
    ) {
        return bookingService.updateStatus(user, id, req.status());
    }

    // ---- payment (self-reported, UPI-style — see PaymentService) ----

    @GetMapping("/api/bookings/{id}/payment")
    public PaymentResponse getPayment(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return bookingService.getPayment(user, id);
    }

    @PostMapping("/api/bookings/{id}/payment/mark-paid")
    public PaymentResponse markPaid(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody(required = false) MarkPaidRequest req
    ) {
        MarkPaidRequest body = req != null ? req : new MarkPaidRequest(null);
        return bookingService.markPaymentPaid(user, id, body);
    }

    // Driver-side counterpart to markPaid — lets the driver confirm they
    // actually received a rider's (UPI) payment, instead of relying solely
    // on the rider's self-report.
    @PostMapping("/api/bookings/{id}/payment/confirm-received")
    public PaymentResponse confirmPaymentReceived(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody(required = false) MarkPaidRequest req
    ) {
        MarkPaidRequest body = req != null ? req : new MarkPaidRequest(null);
        return bookingService.confirmPaymentReceived(user, id, body);
    }

    // ---- payment (Razorpay Checkout — real money) ----

    @PostMapping("/api/bookings/{id}/payment/razorpay-order")
    public RazorpayOrderResponse createRazorpayOrder(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return bookingService.createRazorpayOrder(user, id);
    }

    @PostMapping("/api/bookings/{id}/payment/razorpay-verify")
    public PaymentResponse verifyRazorpayPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RazorpayVerifyRequest req
    ) {
        return bookingService.verifyRazorpayPayment(user, id, req);
    }
}
