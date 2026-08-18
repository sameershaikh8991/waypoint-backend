package com.waypoint.carpool.repository;

import com.waypoint.carpool.entity.Ride;
import com.waypoint.carpool.entity.User;
import com.waypoint.carpool.entity.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {

    List<Ride> findByDriverOrderByDepartureTimeDesc(User driver);

//    @Query("""
//        SELECT r FROM Ride r
//        WHERE r.status = :status
//          AND r.availableSeats > 0
//          AND (:source IS NULL OR LOWER(r.source) LIKE LOWER(CONCAT('%', :source, '%')))
//          AND (:destination IS NULL OR LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')))
//          AND (:dayStart IS NULL OR r.departureTime >= :dayStart)
//          AND (:dayEnd IS NULL OR r.departureTime <= :dayEnd)
//        ORDER BY r.departureTime ASC
//        """)
//    List<Ride> search(
//            @Param("status") RideStatus status,
//            @Param("source") String source,
//            @Param("destination") String destination,
//            @Param("dayStart") LocalDateTime dayStart,
//            @Param("dayEnd") LocalDateTime dayEnd
//    );

    @Query("""
    SELECT r FROM Ride r
    WHERE r.status = :status
      AND r.availableSeats > 0
      AND (:source = '' OR LOWER(r.source) LIKE LOWER(CONCAT('%', :source, '%')))
      AND (:destination = '' OR LOWER(r.destination) LIKE LOWER(CONCAT('%', :destination, '%')))
      AND r.departureTime >= COALESCE(:dayStart, r.departureTime)
      AND r.departureTime <= COALESCE(:dayEnd, r.departureTime)
    ORDER BY r.departureTime ASC
    """)
    List<Ride> search(
            @Param("status") RideStatus status,
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );

//    @Query(value = """
//    SELECT *
//    FROM rides
//    WHERE status = :status
//      AND available_seats > 0
//      AND (:source IS NULL OR LOWER(source) LIKE LOWER(CONCAT('%', :source, '%')))
//      AND (:destination IS NULL OR LOWER(destination) LIKE LOWER(CONCAT('%', :destination, '%')))
//      AND (:dayStart IS NULL OR departure_time >= :dayStart)
//      AND (:dayEnd IS NULL OR departure_time <= :dayEnd)
//    ORDER BY departure_time ASC
//    """, nativeQuery = true)
//    List<Ride> search(
//            @Param("status") String status,
//            @Param("source") String source,
//            @Param("destination") String destination,
//            @Param("dayStart") LocalDateTime dayStart,
//            @Param("dayEnd") LocalDateTime dayEnd
//    );
}
