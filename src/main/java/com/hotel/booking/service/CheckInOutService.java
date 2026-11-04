package com.hotel.booking.service;

import com.hotel.booking.entity.*;
import com.hotel.booking.exception.BookingNotFoundException;
import com.hotel.booking.exception.InvalidBookingException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.StayRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class CheckInOutService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final StayRecordRepository stayRecordRepository;

    public CheckInOutService(BookingRepository bookingRepository,
                             RoomRepository roomRepository,
                             StayRecordRepository stayRecordRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.stayRecordRepository = stayRecordRepository;
    }

    public StayRecord checkIn(Long bookingId, String clerkName) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingException("Cannot check in: Booking status is " + booking.getBookingStatus());
        }

        Room room = booking.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        bookingRepository.save(booking);

        StayRecord record = stayRecordRepository.findByBookingId(bookingId)
                .orElse(new StayRecord());

        record.setBooking(booking);
        record.setActualCheckIn(LocalDateTime.now());
        record.setCheckedInBy(clerkName != null ? clerkName : "Front Desk Staff");

        return stayRecordRepository.save(record);
    }

    public StayRecord checkOut(Long bookingId, String clerkName) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new InvalidBookingException("Cannot check out: Booking status is " + booking.getBookingStatus());
        }

        Room room = booking.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(room);

        booking.setBookingStatus(BookingStatus.CHECKED_OUT);
        bookingRepository.save(booking);

        StayRecord record = stayRecordRepository.findByBookingId(bookingId)
                .orElse(new StayRecord());

        record.setBooking(booking);
        record.setActualCheckOut(LocalDateTime.now());
        record.setCheckedOutBy(clerkName != null ? clerkName : "Front Desk Staff");

        return stayRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public StayRecord getStayRecordByBookingId(Long bookingId) {
        return stayRecordRepository.findByBookingId(bookingId).orElse(null);
    }
}
