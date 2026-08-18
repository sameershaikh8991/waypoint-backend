package com.waypoint.carpool.dto.ride;

import com.waypoint.carpool.entity.User;

public record DriverSummary(Long id, String fullName, String phone, double avgRating) {
    public static DriverSummary from(User u) {
        return new DriverSummary(u.getId(), u.getFullName(), u.getPhone(), u.getAvgRating());
    }
}
