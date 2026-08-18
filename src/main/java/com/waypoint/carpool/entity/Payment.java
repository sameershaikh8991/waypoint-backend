package com.waypoint.carpool.entity;

import com.waypoint.carpool.entity.enums.PaymentStatus;
import com.waypoint.carpool.entity.enums.PayoutStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A payment record for a confirmed booking. Supports two methods:
 *  - UPI: the rider pays the driver directly (outside the app) and
 *    self-reports it here; nothing is verified against a bank.
 *  - RAZORPAY: the rider pays the platform through Razorpay Checkout, the
 *    signature is verified server-side, and the driver's share is tracked
 *    via payoutStatus until the platform forwards it separately.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private RideBooking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_id", nullable = false)
    private User payee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String method = "UPI";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // Optional UPI transaction reference number the rider types in themselves
    // when marking as paid. Not verified against any bank/UPI provider.
    private String transactionRef;

    private Instant paidAt;

    // --- Razorpay ---
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // --- Platform commission split, snapshotted at payment-creation time
    // (this booking's amount x the ride's commission rate). driverPayoutAmount
    // = amount - platformCommissionAmount.
    @Column(precision = 10, scale = 2)
    private BigDecimal platformCommissionAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal driverPayoutAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus payoutStatus = PayoutStatus.NOT_APPLICABLE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RideBooking getBooking() { return booking; }
    public void setBooking(RideBooking booking) { this.booking = booking; }

    public User getPayer() { return payer; }
    public void setPayer(User payer) { this.payer = payer; }

    public User getPayee() { return payee; }
    public void setPayee(User payee) { this.payee = payee; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }

    public BigDecimal getPlatformCommissionAmount() { return platformCommissionAmount; }
    public void setPlatformCommissionAmount(BigDecimal platformCommissionAmount) { this.platformCommissionAmount = platformCommissionAmount; }

    public BigDecimal getDriverPayoutAmount() { return driverPayoutAmount; }
    public void setDriverPayoutAmount(BigDecimal driverPayoutAmount) { this.driverPayoutAmount = driverPayoutAmount; }

    public PayoutStatus getPayoutStatus() { return payoutStatus; }
    public void setPayoutStatus(PayoutStatus payoutStatus) { this.payoutStatus = payoutStatus; }

    public Instant getCreatedAt() { return createdAt; }
}
