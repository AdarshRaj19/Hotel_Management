package com.hotel.booking.service;

import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.entity.*;
import com.hotel.booking.exception.InvalidBookingException;
import com.hotel.booking.exception.RoomNotAvailableException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Unit Tests")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testUser = new User("Rahul Sharma", "rahul@example.com", "pass123", "+91 9999999999", Role.ROLE_CUSTOMER);
        testUser.setId(1L);

        RoomType roomType = new RoomType("Double Room", "Spacious room", 2500.0);
        testRoom = new Room("201", roomType, 2500.0, 2, "Double bed room", RoomStatus.AVAILABLE);
        testRoom.setId(10L);
    }

    @Test
    @DisplayName("Should create booking successfully with accurate tariff calculation")
    void testCreateBookingSuccess() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5); // 3 nights

        BookingRequest request = new BookingRequest(10L, checkIn, checkOut, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findOverlappingBookings(10L, checkIn, checkOut)).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(1L, request);

        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus());
        assertEquals(3 * 2500.0, booking.getTotalAmount()); // 7500.0
        assertEquals(2, booking.getNumberOfGuests());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking if check-in date is in the past")
    void testCreateBookingPastCheckInDate() {
        LocalDate checkIn = LocalDate.now().minusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        BookingRequest request = new BookingRequest(10L, checkIn, checkOut, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    @DisplayName("Should reject booking if check-out date is not after check-in date")
    void testCreateBookingInvalidCheckOutDate() {
        LocalDate checkIn = LocalDate.now().plusDays(3);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        BookingRequest request = new BookingRequest(10L, checkIn, checkOut, 2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    @DisplayName("Should reject booking if guest count exceeds room capacity")
    void testCreateBookingExceedsCapacity() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        BookingRequest request = new BookingRequest(10L, checkIn, checkOut, 4); // Room capacity is 2

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));

        assertThrows(InvalidBookingException.class, () -> bookingService.createBooking(1L, request));
    }

    @Test
    @DisplayName("Should reject booking if dates overlap with existing confirmed booking")
    void testCreateBookingOverlappingCollision() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        BookingRequest request = new BookingRequest(10L, checkIn, checkOut, 2);

        Booking existing = new Booking(testUser, testRoom, checkIn, checkOut, 2, 7500.0, BookingStatus.CONFIRMED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(testRoom));
        when(bookingRepository.findOverlappingBookings(10L, checkIn, checkOut)).thenReturn(List.of(existing));

        assertThrows(RoomNotAvailableException.class, () -> bookingService.createBooking(1L, request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should successfully cancel confirmed booking")
    void testCancelBooking() {
        Booking booking = new Booking(testUser, testRoom, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2, 5000.0, BookingStatus.CONFIRMED);
        booking.setId(50L);

        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(50L, 1L, false);

        assertEquals(BookingStatus.CANCELLED, cancelled.getBookingStatus());
        verify(bookingRepository, times(1)).save(booking);
    }
}
