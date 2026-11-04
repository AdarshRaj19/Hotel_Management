package com.hotel.booking.controller;

import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.StayRecord;
import com.hotel.booking.entity.User;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.CheckInOutService;
import com.hotel.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;
    private final UserService userService;
    private final CheckInOutService checkInOutService;

    public BookingApiController(BookingService bookingService,
                                UserService userService,
                                CheckInOutService checkInOutService) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.checkInOutService = checkInOutService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request,
                                                         @RequestParam(value = "userId", required = false) Long userId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long targetUserId = userId;
        if (targetUserId == null && userDetails != null) {
            User user = userService.getUserByEmail(userDetails.getUsername());
            targetUserId = user.getId();
        }
        if (targetUserId == null) {
            targetUserId = 3L;
        }

        Booking booking = bookingService.createBooking(targetUserId, request);
        return new ResponseEntity<>(BookingResponse.fromEntity(booking), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        Booking booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(BookingResponse.fromEntity(booking));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(@RequestParam(value = "userId", required = false) Long userId,
                                                               @AuthenticationPrincipal UserDetails userDetails) {
        Long targetUserId = userId;
        if (targetUserId == null && userDetails != null) {
            User user = userService.getUserByEmail(userDetails.getUsername());
            targetUserId = user.getId();
        }
        if (targetUserId == null) {
            targetUserId = 3L;
        }

        return ResponseEntity.ok(bookingService.getBookingResponsesByUserId(targetUserId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id,
                                                         @RequestParam(value = "userId", required = false) Long userId,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long targetUserId = userId != null ? userId : 3L;
        boolean isStaff = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTION"));

        Booking cancelled = bookingService.cancelBooking(id, targetUserId, isStaff || userId != null);
        return ResponseEntity.ok(BookingResponse.fromEntity(cancelled));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<StayRecord> checkIn(@PathVariable Long id,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        String clerk = userDetails != null ? userDetails.getUsername() : "API Receptionist";
        StayRecord stay = checkInOutService.checkIn(id, clerk);
        return ResponseEntity.ok(stay);
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<StayRecord> checkOut(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        String clerk = userDetails != null ? userDetails.getUsername() : "API Receptionist";
        StayRecord stay = checkInOutService.checkOut(id, clerk);
        return ResponseEntity.ok(stay);
    }
}
