package com.waypoint.carpool.dto.auth;

import com.waypoint.carpool.entity.User;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        boolean isDriver,
        double avgRating,
        String upiId
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getUsername(), u.getPhone(), u.isDriver(), u.getAvgRating(), u.getUpiId());
    }
}
