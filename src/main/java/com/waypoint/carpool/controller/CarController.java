package com.waypoint.carpool.controller;

import com.waypoint.carpool.dto.car.CarRequest;
import com.waypoint.carpool.dto.car.CarResponse;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.service.CarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @GetMapping("/mine")
    public List<CarResponse> myCars(@AuthenticationPrincipal User user) {
        return carService.myCars(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarResponse addCar(@AuthenticationPrincipal User user, @Valid @RequestBody CarRequest req) {
        return carService.addCar(user, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCar(@AuthenticationPrincipal User user, @PathVariable Long id) {
        carService.deleteCar(user, id);
    }
}
