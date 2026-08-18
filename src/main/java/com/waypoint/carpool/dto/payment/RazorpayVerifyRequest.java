package com.waypoint.carpool.dto.payment;

import jakarta.validation.constraints.NotBlank;

/**
 * The three fields Razorpay Checkout hands back to the frontend in its
 * success callback. All three are required to verify the HMAC signature.
 */
public record RazorpayVerifyRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature
) {}
