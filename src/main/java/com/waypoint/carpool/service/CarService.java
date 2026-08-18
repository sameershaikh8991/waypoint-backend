package com.waypoint.carpool.service;

import com.waypoint.carpool.dto.car.CarRequest;
import com.waypoint.carpool.dto.car.CarResponse;
import com.waypoint.carpool.entity.Car;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.exception.ForbiddenException;
import com.waypoint.carpool.exception.ResourceNotFoundException;
import com.waypoint.carpool.repository.CarRepository;
import com.waypoint.carpool.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarService {

    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public CarService(CarRepository carRepository, UserRepository userRepository) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    public List<CarResponse> myCars(User owner) {
        return carRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream().map(CarResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public CarResponse addCar(User owner, CarRequest req) {
        Car car = new Car();
        car.setOwner(owner);
        car.setMake(req.make());
        car.setModel(req.model());
        car.setColor(req.color());
        car.setPlateNumber(req.plateNumber().toUpperCase());
        car.setSeats(req.seats());
        car = carRepository.save(car);

        // The first registered car makes the user a driver
        if (!owner.isDriver()) {
            owner.setDriver(true);
            userRepository.save(owner);
        }

        return CarResponse.from(car);
    }

    @Transactional
    public void deleteCar(User owner, Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (!car.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You can only remove your own cars");
        }
        carRepository.delete(car);
    }

    public Car getOwnedCarOrThrow(User owner, Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));
        if (!car.getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenException("You can only use your own car to offer a ride");
        }
        return car;
    }
}
