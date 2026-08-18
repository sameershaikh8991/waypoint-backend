package com.waypoint.carpool.dto.car;

import com.waypoint.carpool.entity.Car;

public record CarResponse(
        Long id,
        String make,
        String model,
        String color,
        String plateNumber,
        int seats
) {
    public static CarResponse from(Car c) {
        return new CarResponse(c.getId(), c.getMake(), c.getModel(), c.getColor(), c.getPlateNumber(), c.getSeats());
    }
}
