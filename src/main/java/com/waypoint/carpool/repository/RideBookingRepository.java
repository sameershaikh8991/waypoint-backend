package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.RideBooking;
import com.waypoint.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RideBookingRepository extends JpaRepository<RideBooking, Long> {
    List<RideBooking> findByRide(Ride ride);
    List<RideBooking> findByRiderOrderByCreatedAtDesc(User rider);
    Optional<RideBooking> findByRideAndRider(Ride ride, User rider);
}
