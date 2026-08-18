package com.waypoint.carpool.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpiUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^[\\w.\\-]{2,256}@[a-zA-Z][\\w.\\-]{1,64}$", message = "Doesn't look like a valid UPI ID (e.g. name@bank)")
        String upiId
) {}
