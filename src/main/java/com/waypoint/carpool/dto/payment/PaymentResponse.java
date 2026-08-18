package com.waypoint.carpool.dto.payment;

import com.waypoint.carpool.entity.Payment;
import com.waypoint.carpool.entity.enums.PaymentStatus;
import com.waypoint.carpool.entity.enums.PayoutStatus;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        String payeeUpiId,
        String payeeName,
        String upiDeepLink,
        String transactionRef,
        Instant paidAt,
        String razorpayOrderId,
        BigDecimal platformCommissionAmount,
        BigDecimal driverPayoutAmount,
        PayoutStatus payoutStatus
) {
    public static PaymentResponse from(Payment p) {
        String upiLink = null;
        String upiId = p.getPayee().getUpiId();
        if (upiId != null && !upiId.isBlank()) {
            String note = URLEncoder.encode("Waypoint ride #" + p.getBooking().getRide().getId(), StandardCharsets.UTF_8);
            String payeeName = URLEncoder.encode(p.getPayee().getFullName(), StandardCharsets.UTF_8);
            upiLink = "upi://pay?pa=" + upiId + "&pn=" + payeeName
                    + "&am=" + p.getAmount().toPlainString() + "&cu=INR&tn=" + note;
        }

        return new PaymentResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getAmount(),
                p.getMethod(),
                p.getStatus(),
                upiId,
                p.getPayee().getFullName(),
                upiLink,
                p.getTransactionRef(),
                p.getPaidAt(),
                p.getRazorpayOrderId(),
                p.getPlatformCommissionAmount(),
                p.getDriverPayoutAmount(),
                p.getPayoutStatus()
        );
    }
}
