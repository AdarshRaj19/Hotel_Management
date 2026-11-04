package com.hotel.booking.service;

import com.hotel.booking.ds.CustomCircularQueue;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.BookingStatus;
import com.hotel.booking.exception.BookingNotFoundException;
import com.hotel.booking.exception.InvalidBookingException;
import com.hotel.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service managing customer check-in waiting queues using our custom Circular Queue data structure.
 */
@Service
public class CheckInQueueService {

    private final CustomCircularQueue<Booking> waitingQueue;
    private final BookingRepository bookingRepository;

    public CheckInQueueService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
        // Capacity of 25 waiting guests at reception
        this.waitingQueue = new CustomCircularQueue<>(25);
    }

    public synchronized void enqueueCustomer(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException("Cannot enqueue booking with status: " + booking.getBookingStatus());
        }

        // Avoid duplicate entry in queue
        boolean alreadyQueued = waitingQueue.toList().stream()
                .anyMatch(b -> b.getId().equals(bookingId));

        if (alreadyQueued) {
            throw new InvalidBookingException("Booking #" + bookingId + " is already in the check-in queue.");
        }

        waitingQueue.enqueue(booking);
    }

    public synchronized Booking serveNext() {
        if (waitingQueue.isEmpty()) {
            return null;
        }
        return waitingQueue.dequeue();
    }

    public synchronized Booking peekNext() {
        return waitingQueue.peek();
    }

    public synchronized List<Booking> getWaitingQueue() {
        return waitingQueue.toList();
    }

    public synchronized int getQueueSize() {
        return waitingQueue.size();
    }

    public synchronized void clearQueue() {
        waitingQueue.clear();
    }
}
