package com.waypoint.carpool.entity.enums;

/**
 * Tracks whether the driver's share of a Razorpay payment still needs to be
 * forwarded to them. Only meaningful for RAZORPAY payments, where the money
 * lands in the platform's account first (not a direct rider -> driver
 * transfer). UPI payments never leave PENDING_PAYOUT territory because the
 * driver is paid directly, outside the app.
 */
public enum PayoutStatus {
    NOT_APPLICABLE,  // UPI (or any) direct rider -> driver payment; nothing to forward
    PENDING_PAYOUT,  // Razorpay payment captured; platform still owes the driver their share
    PAID_OUT         // platform has sent the driver their share (marked from the admin dashboard)
}
