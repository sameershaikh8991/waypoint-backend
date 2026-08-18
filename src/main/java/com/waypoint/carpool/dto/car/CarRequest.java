package com.waypoint.carpool.dto.car;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CarRequest(
        @NotBlank String make,
        @NotBlank String model,
        String color,
        @NotBlank String plateNumber,
        @Min(1) @Max(8) int seats
) {}
