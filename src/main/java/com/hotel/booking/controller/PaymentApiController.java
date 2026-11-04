package com.hotel.booking.controller;

import com.hotel.booking.dto.BillSummary;
import com.hotel.booking.dto.PaymentRequest;
import com.hotel.booking.dto.PaymentResponse;
import com.hotel.booking.entity.Payment;
import com.hotel.booking.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

    private final PaymentService paymentService;

    public PaymentApiController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/bill/{bookingId}")
    public ResponseEntity<BillSummary> getBill(@PathVariable Long bookingId,
                                               @RequestParam(value = "additionalCharges", defaultValue = "0.0") Double additionalCharges) {
        return ResponseEntity.ok(paymentService.calculateBill(bookingId, additionalCharges));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(request);
        return new ResponseEntity<>(PaymentResponse.fromEntity(payment), HttpStatus.CREATED);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long bookingId) {
        Payment payment = paymentService.getPaymentByBookingId(bookingId);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
    }
}
