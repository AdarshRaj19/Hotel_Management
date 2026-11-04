package com.hotel.booking.service;

import com.hotel.booking.entity.*;
import com.hotel.booking.exception.BookingNotFoundException;
import com.hotel.booking.exception.InvalidBookingException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.StayRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckInOutService Unit Tests")
class CheckInOutServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private StayRecordRepository stayRecordRepository;

    @InjectMocks
    private CheckInOutService checkInOutService;

    // User uses: id, fullName, email, password, phone, role (NO setName())
    // RoomType uses: id, name, description, defaultPrice (NO setCapacity())
    private Room room;
    private Booking confirmedBooking;
    private Booking checkedInBooking;
    private User customer;

    @BeforeEach
    void setUp() {
        RoomType type = new RoomType("Standard", "Standard room", 3000.0);
        type.setId(1L);

        room = new Room();
        room.setId(1L);
        room.setRoomNumber("101");
        room.setRoomType(type);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPricePerNight(3000.0);

        customer = new User();
        customer.setId(1L);
        customer.setEmail("customer@hotelease.com");
        customer.setFullName("John Doe");

        confirmedBooking = new Booking();
        confirmedBooking.setId(1L);
        confirmedBooking.setRoom(room);
        confirmedBooking.setUser(customer);
        confirmedBooking.setBookingStatus(BookingStatus.CONFIRMED);
        confirmedBooking.setCheckInDate(LocalDate.now());
        confirmedBooking.setCheckOutDate(LocalDate.now().plusDays(2));
        confirmedBooking.setTotalAmount(6000.0);

        checkedInBooking = new Booking();
        checkedInBooking.setId(2L);
        checkedInBooking.setRoom(room);
        checkedInBooking.setUser(customer);
        checkedInBooking.setBookingStatus(BookingStatus.CHECKED_IN);
        checkedInBooking.setCheckInDate(LocalDate.now().minusDays(1));
        checkedInBooking.setCheckOutDate(LocalDate.now().plusDays(1));
        checkedInBooking.setTotalAmount(6000.0);
    }

    // ─── checkIn ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkIn: CONFIRMED booking → CHECKED_IN, room → OCCUPIED, StayRecord created")
    void checkIn_confirmedBooking_success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(stayRecordRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(stayRecordRepository.save(any(StayRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        checkInOutService.checkIn(1L, "Staff");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getBookingStatus()).isEqualTo(BookingStatus.CHECKED_IN);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getStatus()).isEqualTo(RoomStatus.OCCUPIED);

        verify(stayRecordRepository).save(any(StayRecord.class));
    }

    @Test
    @DisplayName("checkIn: booking not found throws BookingNotFoundException")
    void checkIn_bookingNotFound_throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkInOutService.checkIn(999L, "Staff"))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("checkIn: already CHECKED_IN booking throws InvalidBookingException")
    void checkIn_alreadyCheckedIn_throws() {
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(checkedInBooking));

        assertThatThrownBy(() -> checkInOutService.checkIn(2L, "Staff"))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("checkIn: CANCELLED booking throws InvalidBookingException")
    void checkIn_cancelledBooking_throws() {
        confirmedBooking.setBookingStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> checkInOutService.checkIn(1L, "Staff"))
                .isInstanceOf(InvalidBookingException.class);
    }

    // ─── checkOut ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkOut: CHECKED_IN booking → CHECKED_OUT, room → AVAILABLE")
    void checkOut_checkedInBooking_success() {
        room.setStatus(RoomStatus.OCCUPIED);
        checkedInBooking.setRoom(room);

        when(bookingRepository.findById(2L)).thenReturn(Optional.of(checkedInBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(checkedInBooking);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(stayRecordRepository.findByBookingId(2L)).thenReturn(Optional.empty());
        when(stayRecordRepository.save(any(StayRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        checkInOutService.checkOut(2L, "Staff");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getBookingStatus()).isEqualTo(BookingStatus.CHECKED_OUT);

        ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getStatus()).isEqualTo(RoomStatus.AVAILABLE);
    }

    @Test
    @DisplayName("checkOut: booking not found throws BookingNotFoundException")
    void checkOut_bookingNotFound_throws() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkInOutService.checkOut(999L, "Staff"))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    @DisplayName("checkOut: CONFIRMED (not checked in) booking throws InvalidBookingException")
    void checkOut_notCheckedIn_throws() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> checkInOutService.checkOut(1L, "Staff"))
                .isInstanceOf(InvalidBookingException.class);
    }

    @Test
    @DisplayName("checkOut: CHECKED_OUT booking throws InvalidBookingException")
    void checkOut_alreadyCheckedOut_throws() {
        checkedInBooking.setBookingStatus(BookingStatus.CHECKED_OUT);
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(checkedInBooking));

        assertThatThrownBy(() -> checkInOutService.checkOut(2L, "Staff"))
                .isInstanceOf(InvalidBookingException.class);
    }
}
