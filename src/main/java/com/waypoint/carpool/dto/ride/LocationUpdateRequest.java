package com.waypoint.carpool.dto.ride;

import jakarta.validation.constraints.NotNull;

public record LocationUpdateRequest(
        @NotNull Double lat,
        @NotNull Double lng
) {}
