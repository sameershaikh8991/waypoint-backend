package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.ride.LocationUpdateRequest;
import com.waypoint.carpool.dto.ride.RideLocationResponse;
import com.waypoint.carpool.dto.ride.RideRequest;
import com.waypoint.carpool.dto.ride.RideResponse;
import com.waypoint.carpool.dto.ride.RideStatusUpdateRequest;
import com.waypoint.carpool.dto.ride.RideUpdateRequest;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.RideService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/search-ride")
    public List<RideResponse> search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination
    ) {
        return rideService.search(source, destination);
    }

    @GetMapping("/{id}")
    public RideResponse getById(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return rideService.getById(id, user);
    }

    @GetMapping("/mine/driving")
    public List<RideResponse> myDrivingRides(@AuthenticationPrincipal User user) {
        return rideService.myDrivingRides(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RideResponse createRide(@AuthenticationPrincipal User user, @Valid @RequestBody RideRequest req) {
        return rideService.createRide(user, req);
    }

    // Driver edits a ride's details — only while it's still SCHEDULED.
    @PatchMapping("/{id}")
    public RideResponse updateRide(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RideUpdateRequest req
    ) {
        return rideService.updateRide(user, id, req);
    }

    @PatchMapping("/{id}/status")
    public RideResponse updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody RideStatusUpdateRequest req
    ) {
        return rideService.updateStatus(user, id, req.status());
    }

    // Driver pushes their live GPS position while the ride is ONGOING.
    @PutMapping("/{id}/location")
    public RideLocationResponse updateLocation(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody LocationUpdateRequest req
    ) {
        return rideService.updateDriverLocation(user, id, req);
    }

    // Driver or a rider with a booking polls the current live location.
    @GetMapping("/{id}/location")
    public RideLocationResponse getLocation(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return rideService.getDriverLocation(user, id);
    }
}
