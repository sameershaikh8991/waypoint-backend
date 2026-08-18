package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.Car;
import com.waypoint.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByOwnerOrderByCreatedAtDesc(User owner);
}
