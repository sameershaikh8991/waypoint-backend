package com.waypoint.carpool.dto.auth;

import jakarta.validation.constraints.NotBlank;

// idToken is the JWT "credential" Google Identity Services hands back to the
// frontend after the user picks an account — NOT an access token, and NOT
// something the backend should trust without verifying its signature.
public record GoogleAuthRequest(
        @NotBlank String idToken
) {}
