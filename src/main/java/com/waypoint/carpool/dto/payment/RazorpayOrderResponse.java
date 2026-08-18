package com.waypoint.carpool.dto.payment;

/**
 * Everything the frontend needs to open Razorpay Checkout. amountPaise is
 * in the smallest currency unit (paise), which is what Checkout expects.
 */
public record RazorpayOrderResponse(
        String razorpayOrderId,
        int amountPaise,
        String currency,
        String keyId,
        String name,
        String description,
        Long bookingId
) {}
