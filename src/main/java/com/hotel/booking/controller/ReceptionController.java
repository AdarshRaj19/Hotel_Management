package com.hotel.booking.controller;

import com.hotel.booking.dto.BillSummary;
import com.hotel.booking.dto.PaymentRequest;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.Payment;
import com.hotel.booking.entity.PaymentMethod;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.StayRecord;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.service.CheckInOutService;
import com.hotel.booking.service.CheckInQueueService;
import com.hotel.booking.service.PaymentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reception")
public class ReceptionController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final CheckInOutService checkInOutService;
    private final PaymentService paymentService;
    private final CheckInQueueService checkInQueueService;

    public ReceptionController(BookingRepository bookingRepository,
                               RoomRepository roomRepository,
                               CheckInOutService checkInOutService,
                               PaymentService paymentService,
                               CheckInQueueService checkInQueueService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.checkInOutService = checkInOutService;
        this.paymentService = paymentService;
        this.checkInQueueService = checkInQueueService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        List<Booking> arrivals = bookingRepository.findArrivalsByDate(today);
        List<Booking> departures = bookingRepository.findDeparturesByDate(today);

        long availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE).size();
        long occupiedRooms = roomRepository.findByStatus(RoomStatus.OCCUPIED).size();

        model.addAttribute("today", today);
        model.addAttribute("arrivals", arrivals);
        model.addAttribute("departures", departures);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("queueSize", checkInQueueService.getQueueSize());

        return "reception-dashboard";
    }

    @GetMapping("/check-in-out")
    public String checkInOutView(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Booking> bookings = bookingRepository.findAll();

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            bookings = bookings.stream()
                    .filter(b -> b.getId().toString().contains(q) ||
                                 b.getUser().getFullName().toLowerCase().contains(q) ||
                                 b.getRoom().getRoomNumber().toLowerCase().contains(q))
                    .toList();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("search", search);
        model.addAttribute("waitingQueue", checkInQueueService.getWaitingQueue());
        model.addAttribute("queueSize", checkInQueueService.getQueueSize());
        model.addAttribute("nextGuest", checkInQueueService.peekNext());

        return "check-in-out";
    }

    @PostMapping("/queue/enqueue/{id}")
    public String enqueueGuest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            checkInQueueService.enqueueCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " added to the waiting queue.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reception/check-in-out";
    }

    @PostMapping("/queue/serve")
    public String serveNextGuest(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            Booking next = checkInQueueService.serveNext();
            if (next == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Waiting queue is empty.");
                return "redirect:/reception/check-in-out";
            }
            String clerk = userDetails != null ? userDetails.getUsername() : "Receptionist";
            checkInOutService.checkIn(next.getId(), clerk);
            redirectAttributes.addFlashAttribute("successMessage", "Served and checked in guest: " + next.getUser().getFullName() + " for Room #" + next.getRoom().getRoomNumber());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reception/check-in-out";
    }

    @PostMapping("/check-in/{id}")
    public String processCheckIn(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            String clerk = userDetails != null ? userDetails.getUsername() : "Receptionist";
            StayRecord record = checkInOutService.checkIn(id, clerk);
            redirectAttributes.addFlashAttribute("successMessage", "Guest checked in successfully for Booking #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reception/check-in-out";
    }

    @PostMapping("/check-out/{id}")
    public String processCheckOut(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        try {
            String clerk = userDetails != null ? userDetails.getUsername() : "Receptionist";
            StayRecord record = checkInOutService.checkOut(id, clerk);
            redirectAttributes.addFlashAttribute("successMessage", "Guest checked out successfully! Proceeding to invoice.");
            return "redirect:/reception/bill/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reception/check-in-out";
        }
    }

    @GetMapping("/bill/{bookingId}")
    public String viewBill(@PathVariable Long bookingId,
                           @RequestParam(value = "additionalCharges", defaultValue = "0.0") Double additionalCharges,
                           Model model) {
        BillSummary bill = paymentService.calculateBill(bookingId, additionalCharges);
        Payment existingPayment = paymentService.getPaymentByBookingId(bookingId);
        StayRecord stayRecord = checkInOutService.getStayRecordByBookingId(bookingId);

        model.addAttribute("bill", bill);
        model.addAttribute("payment", existingPayment);
        model.addAttribute("stayRecord", stayRecord);
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "payment";
    }

    @PostMapping("/pay")
    public String processPayment(@ModelAttribute PaymentRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            Payment payment = paymentService.processPayment(request);
            redirectAttributes.addFlashAttribute("successMessage", "Payment of ₹" + payment.getAmount() + " processed successfully! Ref: " + payment.getTransactionReference());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reception/bill/" + request.getBookingId();
    }
}
