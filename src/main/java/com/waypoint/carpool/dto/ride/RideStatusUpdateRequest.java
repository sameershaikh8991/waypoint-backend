package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.enums.RideStatus;
import jakarta.validation.constraints.NotNull;

public record RideStatusUpdateRequest(@NotNull RideStatus status) {}
