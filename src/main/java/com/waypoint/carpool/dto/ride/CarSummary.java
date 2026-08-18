package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.Car;

public record CarSummary(Long id, String make, String model, String color, String plateNumber, int seats) {
    public static CarSummary from(Car c) {
        return new CarSummary(c.getId(), c.getMake(), c.getModel(), c.getColor(), c.getPlateNumber(), c.getSeats());
    }
}
