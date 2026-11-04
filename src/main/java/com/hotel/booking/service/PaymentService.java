package com.hotel.booking.service;

import com.hotel.booking.dto.BillSummary;
import com.hotel.booking.dto.PaymentRequest;
import com.hotel.booking.dto.PaymentResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.Payment;
import com.hotel.booking.entity.PaymentStatus;
import com.hotel.booking.exception.BookingNotFoundException;
import com.hotel.booking.exception.PaymentFailedException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public BillSummary calculateBill(Long bookingId, Double additionalCharges) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + bookingId));

        double addCharges = additionalCharges != null ? additionalCharges : 0.0;
        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }

        double roomTariff = booking.getTotalAmount();
        double subtotal = roomTariff + addCharges;
        double taxRate = 0.12; // 12% GST
        double taxAmount = Math.round((subtotal * taxRate) * 100.0) / 100.0;
        double grandTotal = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

        BillSummary bill = new BillSummary();
        bill.setBookingId(booking.getId());
        bill.setGuestName(booking.getUser().getFullName());
        bill.setGuestEmail(booking.getUser().getEmail());
        bill.setRoomNumber(booking.getRoom().getRoomNumber());
        bill.setRoomTypeName(booking.getRoom().getRoomType().getName());
        bill.setNumberOfNights(nights);
        bill.setPricePerNight(booking.getRoom().getPricePerNight());
        bill.setRoomTariff(roomTariff);
        bill.setAdditionalCharges(addCharges);
        bill.setTaxRate(taxRate);
        bill.setTaxAmount(taxAmount);
        bill.setGrandTotal(grandTotal);

        return bill;
    }

    public Payment processPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            throw new PaymentFailedException("Payment has already been processed for booking #" + request.getBookingId());
        }

        String transactionRef = request.getTransactionReference();
        if (transactionRef == null || transactionRef.isBlank()) {
            transactionRef = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(transactionRef);
        payment.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId).orElse(null);
    }
}
