package com.hotel.booking.controller;

import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.service.CheckInOutService;
import com.hotel.booking.service.CheckInQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
public class QueueApiController {

    private final CheckInQueueService checkInQueueService;
    private final CheckInOutService checkInOutService;

    public QueueApiController(CheckInQueueService checkInQueueService,
                              CheckInOutService checkInOutService) {
        this.checkInQueueService = checkInQueueService;
        this.checkInOutService = checkInOutService;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getWaitingQueue() {
        List<BookingResponse> list = checkInQueueService.getWaitingQueue().stream()
                .map(BookingResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{bookingId}")
    public ResponseEntity<String> enqueue(@PathVariable Long bookingId) {
        checkInQueueService.enqueueCustomer(bookingId);
        return ResponseEntity.ok("Booking #" + bookingId + " added to the waiting queue.");
    }

    @PostMapping("/serve")
    public ResponseEntity<BookingResponse> serveNext(@AuthenticationPrincipal UserDetails userDetails) {
        Booking next = checkInQueueService.serveNext();
        if (next == null) {
            return ResponseEntity.noContent().build();
        }
        String clerk = userDetails != null ? userDetails.getUsername() : "API Receptionist";
        checkInOutService.checkIn(next.getId(), clerk);
        return ResponseEntity.ok(BookingResponse.fromEntity(next));
    }
}
