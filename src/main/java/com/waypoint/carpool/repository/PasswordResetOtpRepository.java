package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.PasswordResetOtp;
import com.waypoint.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findTopByUserOrderByCreatedAtDesc(User user);
}
