package com.hotel.booking.service;

import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.dto.BookingResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.BookingStatus;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.User;
import com.hotel.booking.exception.BookingNotFoundException;
import com.hotel.booking.exception.InvalidBookingException;
import com.hotel.booking.exception.ResourceNotFoundException;
import com.hotel.booking.exception.RoomNotAvailableException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          RoomRepository roomRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public Booking createBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException("Room #" + room.getRoomNumber() + " is currently under maintenance.");
        }

        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckOutDate();

        if (checkIn == null || checkOut == null) {
            throw new InvalidBookingException("Both check-in and check-out dates are required.");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingException("Check-out date must be after check-in date.");
        }

        if (checkIn.isBefore(LocalDate.now())) {
            throw new InvalidBookingException("Check-in date cannot be in the past.");
        }

        if (request.getNumberOfGuests() > room.getCapacity()) {
            throw new InvalidBookingException("Number of guests (" + request.getNumberOfGuests() +
                    ") exceeds room capacity (" + room.getCapacity() + ").");
        }

        // Check overlapping bookings
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(room.getId(), checkIn, checkOut);
        if (!overlapping.isEmpty()) {
            throw new RoomNotAvailableException("Room #" + room.getRoomNumber() + " is already reserved for the selected dates.");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double totalAmount = nights * room.getPricePerNight();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalAmount(totalAmount);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(Long bookingId, Long userId, boolean isStaffOrAdmin) {
        Booking booking = getBookingById(bookingId);

        if (!isStaffOrAdmin && !booking.getUser().getId().equals(userId)) {
            throw new InvalidBookingException("You are not authorized to cancel this booking.");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking is already cancelled.");
        }

        if (booking.getBookingStatus() == BookingStatus.CHECKED_OUT) {
            throw new InvalidBookingException("Completed stays cannot be cancelled.");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingResponsesByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
