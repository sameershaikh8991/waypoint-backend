package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.Payment;
import com.waypoint.carpool.entity.RideBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(RideBooking booking);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
