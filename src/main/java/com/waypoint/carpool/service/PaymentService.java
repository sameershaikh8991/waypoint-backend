package com.waypoint.carpool.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.waypoint.carpool.dto.payment.PaymentResponse;
import com.waypoint.carpool.dto.payment.RazorpayOrderResponse;
import com.waypoint.carpool.dto.payment.RazorpayVerifyRequest;
import com.waypoint.carpool.entity.Payment;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.NotificationType;
import com.waypoint.carpool.entity.enums.PaymentStatus;
import com.waypoint.carpool.entity.enums.PayoutStatus;
import com.waypoint.carpool.exception.BadRequestException;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Tracks who owes what once a ride is completed, and whether they've paid.
 * Two payment methods are supported:
 *  - UPI: bookkeeping only. Riders pay drivers directly (outside this app)
 *    and self-report the payment here. No money moves through the app.
 *  - RAZORPAY: real money, via Razorpay Checkout. The rider pays the
 *    platform's Razorpay account; the driver's share is tracked separately
 *    (payoutStatus) until the platform forwards it. See createRazorpayOrder
 *    / verifyRazorpayPayment / handleWebhook below.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;

    public PaymentService(
            PaymentRepository paymentRepository,
            NotificationService notificationService,
            @Value("${app.razorpay.key-id:}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret:}") String razorpayKeySecret,
            @Value("${app.razorpay.webhook-secret:}") String razorpayWebhookSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    /**
     * Called once a ride is marked COMPLETED, for each of its confirmed
     * bookings. Payment is requested only after the ride actually happened
     * — safer for the rider than paying up-front for a ride that might get
     * cancelled partway through. Defaults to UPI; the rider chooses Razorpay
     * instead by starting a Checkout order (createRazorpayOrder).
     */
    @Transactional
    public void ensurePaymentForCompletedRide(RideBooking booking) {
        if (paymentRepository.findByBooking(booking).isPresent()) return;

        BigDecimal amount = booking.getRide().getPricePerSeat()
                .multiply(BigDecimal.valueOf(booking.getSeatsBooked()));

        BigDecimal commissionPercent = booking.getRide().getPlatformCommissionPercent();
        BigDecimal commissionAmount = amount.multiply(commissionPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal driverPayout = amount.subtract(commissionAmount);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPayer(booking.getRider());
        payment.setPayee(booking.getRide().getDriver());
        payment.setAmount(amount);
        payment.setMethod("UPI");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPlatformCommissionAmount(commissionAmount);
        payment.setDriverPayoutAmount(driverPayout);
        payment.setPayoutStatus(PayoutStatus.NOT_APPLICABLE);
        paymentRepository.save(payment);

        notificationService.notify(
                booking.getRider(),
                NotificationType.PAYMENT_REQUESTED,
                "Payment due",
                "Your ride from " + booking.getRide().getSource() + " to " + booking.getRide().getDestination()
                        + " is complete. ₹" + amount.toPlainString() + " is due to the driver.",
                booking.getRide().getId(),
                booking.getId()
        );
    }

    /** Used to enrich the driver's rider list — null if no payment record exists yet. */
    public PaymentStatus statusForBooking(RideBooking booking) {
        return paymentRepository.findByBooking(booking).map(Payment::getStatus).orElse(null);
    }

    public PaymentResponse getForBooking(User caller, Long bookingId, RideBooking booking) {
        boolean isPayer = booking.getRider().getId().equals(caller.getId());
        boolean isPayee = booking.getRide().getDriver().getId().equals(caller.getId());
        if (!isPayer && !isPayee) {
            throw new ForbiddenException("You don't have access to this payment");
        }
        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("No payment has been generated for this booking yet — it's only created once the driver marks the ride completed."));
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse markPaid(User rider, RideBooking booking, String transactionRef) {
        if (!booking.getRider().getId().equals(rider.getId())) {
            throw new ForbiddenException("Only the rider who booked this seat can mark it paid");
        }

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record exists for this booking yet"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This payment is already marked as paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionRef(transactionRef);
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        notificationService.notify(
                payment.getPayee(),
                NotificationType.PAYMENT_RECEIVED,
                "Payment marked as received",
                rider.getFullName() + " marked ₹" + payment.getAmount().toPlainString()
                        + " as paid for the ride from " + booking.getRide().getSource()
                        + " to " + booking.getRide().getDestination() + ".",
                booking.getRide().getId(),
                booking.getId()
        );

        return PaymentResponse.from(payment);
    }

    /**
     * Lets the driver confirm, on their side, that a rider's (UPI) payment
     * actually came through — independent of the rider self-reporting it via
     * markPaid. Whichever side acts first marks the payment PAID; the other
     * action then becomes a no-op via the "already paid" guard below.
     */
    @Transactional
    public PaymentResponse confirmReceivedByDriver(User driver, RideBooking booking, String transactionRef) {
        if (!booking.getRide().getDriver().getId().equals(driver.getId())) {
            throw new ForbiddenException("Only the driver for this ride can confirm this payment");
        }

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record exists for this booking yet"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This payment is already marked as paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        if (transactionRef != null && !transactionRef.isBlank()) {
            payment.setTransactionRef(transactionRef);
        }
        payment.setPaidAt(Instant.now());
        payment = paymentRepository.save(payment);

        notificationService.notify(
                payment.getPayer(),
                NotificationType.PAYMENT_RECEIVED,
                "Payment confirmed by driver",
                driver.getFullName() + " confirmed receiving ₹" + payment.getAmount().toPlainString()
                        + " for the ride from " + booking.getRide().getSource()
                        + " to " + booking.getRide().getDestination() + ".",
                booking.getRide().getId(),
                booking.getId()
        );

        return PaymentResponse.from(payment);
    }

    // ---------------- Razorpay ----------------

    /**
     * Starts a Razorpay Checkout payment for a booking: creates an Order on
     * Razorpay's side and returns what the frontend needs to open Checkout.
     * The actual charge only happens if the rider completes the Checkout
     * flow; nothing is marked paid here.
     */
    @Transactional
    public RazorpayOrderResponse createRazorpayOrder(User rider, RideBooking booking) {
        if (!booking.getRider().getId().equals(rider.getId())) {
            throw new ForbiddenException("Only the rider who booked this seat can pay for it");
        }
        if (razorpayKeyId.isBlank() || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Online payment isn't configured on this server yet — set RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET");
        }

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record exists for this booking yet"));
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("This payment is already marked as paid");
        }

        int amountPaise = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "booking_" + booking.getId());
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            payment.setRazorpayOrderId(orderId);
            payment.setMethod("RAZORPAY");
            paymentRepository.save(payment);

            String route = booking.getRide().getSource() + " to " + booking.getRide().getDestination();
            return new RazorpayOrderResponse(orderId, amountPaise, "INR", razorpayKeyId, "Waypoint", "Ride: " + route, booking.getId());
        } catch (RazorpayException e) {
            throw new BadRequestException("Couldn't start the payment right now — try again in a moment");
        }
    }

    /**
     * Called by the frontend after Razorpay Checkout's success callback.
     * Verifies the HMAC signature server-side before trusting the payment —
     * never trust "it succeeded" from the browser alone.
     */
    @Transactional
    public PaymentResponse verifyRazorpayPayment(User rider, RideBooking booking, RazorpayVerifyRequest req) {
        if (!booking.getRider().getId().equals(rider.getId())) {
            throw new ForbiddenException("Only the rider who booked this seat can pay for it");
        }

        Payment payment = paymentRepository.findByBooking(booking)
                .orElseThrow(() -> new ResourceNotFoundException("No payment record exists for this booking yet"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return PaymentResponse.from(payment); // already verified (e.g. webhook beat us to it) — idempotent
        }
        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(req.razorpayOrderId())) {
            throw new BadRequestException("This payment doesn't match an order we created for this booking");
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", req.razorpayOrderId());
        options.put("razorpay_payment_id", req.razorpayPaymentId());
        options.put("razorpay_signature", req.razorpaySignature());

        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (RazorpayException e) {
            valid = false;
        }
        if (!valid) {
            throw new BadRequestException("Payment verification failed — the signature didn't match");
        }

        markRazorpayPaid(payment, req.razorpayPaymentId(), req.razorpaySignature());
        return PaymentResponse.from(payment);
    }

    /**
     * Fallback path for the "payment.captured" webhook, keyed by Razorpay's
     * order id rather than a booking id (that's all the webhook payload
     * gives us). Safety net for cases where the rider's browser closed
     * before the Checkout success callback could reach verifyRazorpayPayment.
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (razorpayWebhookSecret.isBlank()) return; // webhook not configured — verify-on-callback still covers normal flow

        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(payload, signatureHeader, razorpayWebhookSecret);
        } catch (RazorpayException e) {
            valid = false;
        }
        if (!valid) {
            throw new BadRequestException("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        if (!"payment.captured".equals(event.optString("event"))) return;

        JSONObject paymentEntity = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String orderId = paymentEntity.optString("order_id", null);
        String paymentId = paymentEntity.optString("id", null);
        if (orderId == null || paymentId == null) return;

        paymentRepository.findByRazorpayOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PAID) {
                markRazorpayPaid(payment, paymentId, null);
            }
        });
    }

    private void markRazorpayPaid(Payment payment, String razorpayPaymentId, String razorpaySignature) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        if (razorpaySignature != null) payment.setRazorpaySignature(razorpaySignature);
        payment.setTransactionRef(razorpayPaymentId);
        payment.setPaidAt(Instant.now());
        payment.setPayoutStatus(PayoutStatus.PENDING_PAYOUT); // money is with the platform now — driver still needs their cut forwarded
        paymentRepository.save(payment);

        notificationService.notify(
                payment.getPayee(),
                NotificationType.PAYMENT_RECEIVED,
                "Payment received",
                "₹" + payment.getAmount().toPlainString() + " was paid online for the ride from "
                        + payment.getBooking().getRide().getSource() + " to " + payment.getBooking().getRide().getDestination()
                        + ". Your share (₹" + payment.getDriverPayoutAmount().toPlainString() + " after the platform fee) will be sent to you separately.",
                payment.getBooking().getRide().getId(),
                payment.getBooking().getId()
        );
    }
}
