package com.hotel.booking.service;

import com.hotel.booking.dto.BillSummary;
import com.hotel.booking.dto.PaymentRequest;
import com.hotel.booking.entity.*;
import com.hotel.booking.exception.PaymentFailedException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Booking testBooking;

    @BeforeEach
    void setUp() {
        User user = new User("Priya Patel", "priya@example.com", "pass", "123", Role.ROLE_CUSTOMER);
        RoomType type = new RoomType("Deluxe Suite", "Deluxe", 4000.0);
        Room room = new Room("301", type, 4000.0, 3, "View", RoomStatus.AVAILABLE);

        testBooking = new Booking(user, room, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 2, 8000.0, BookingStatus.CHECKED_OUT);
        testBooking.setId(100L);
    }

    @Test
    @DisplayName("Should accurately calculate bill with room tariff, additional charges, and 12% GST")
    void testCalculateBill() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));

        BillSummary bill = paymentService.calculateBill(100L, 500.0);

        assertNotNull(bill);
        assertEquals(8000.0, bill.getRoomTariff());
        assertEquals(500.0, bill.getAdditionalCharges());
        assertEquals(0.12, bill.getTaxRate());

        // Subtotal = 8500, Tax = 8500 * 0.12 = 1020, Grand Total = 9520
        assertEquals(1020.0, bill.getTaxAmount());
        assertEquals(9520.0, bill.getGrandTotal());
    }

    @Test
    @DisplayName("Should process payment and assign COMPLETED status")
    void testProcessPaymentSuccess() {
        PaymentRequest request = new PaymentRequest(100L, 9520.0, PaymentMethod.UPI, "TXN-CUSTOM-001");

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.processPayment(request);

        assertNotNull(payment);
        assertEquals(9520.0, payment.getAmount());
        assertEquals(PaymentMethod.UPI, payment.getPaymentMethod());
        assertEquals(PaymentStatus.COMPLETED, payment.getPaymentStatus());
        assertEquals("TXN-CUSTOM-001", payment.getTransactionReference());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject duplicate payment for the same booking")
    void testDuplicatePaymentThrowsException() {
        PaymentRequest request = new PaymentRequest(100L, 9520.0, PaymentMethod.CASH, "REF123");

        Payment existingPayment = new Payment();
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(testBooking));
        when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(existingPayment));

        assertThrows(PaymentFailedException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
