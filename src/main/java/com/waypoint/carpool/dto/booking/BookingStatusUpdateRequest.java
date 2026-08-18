package com.waypoint.carpool.dto.booking;

import com.waypoint.carpool.entity.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public record BookingStatusUpdateRequest(@NotNull BookingStatus status) {}
