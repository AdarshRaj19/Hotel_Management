package com.hotel.booking.controller;

import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.BookingStatus;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.UserRepository;
import com.hotel.booking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public AdminController(RoomRepository roomRepository,
                           BookingRepository bookingRepository,
                           UserRepository userRepository,
                           BookingService bookingService) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE).size();
        long occupiedRooms = roomRepository.findByStatus(RoomStatus.OCCUPIED).size();
        long totalUsers = userRepository.count();

        List<Booking> allBookings = bookingRepository.findAll();
        long totalBookings = allBookings.size();

        double totalRevenue = allBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED ||
                             b.getBookingStatus() == BookingStatus.CHECKED_IN ||
                             b.getBookingStatus() == BookingStatus.CHECKED_OUT)
                .mapToDouble(Booking::getTotalAmount)
                .sum();

        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("occupiedRooms", occupiedRooms);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("recentBookings", allBookings.stream().limit(5).toList());

        return "admin-dashboard";
    }

    @GetMapping("/bookings")
    public String manageBookings(@RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "status", required = false) BookingStatus status,
                                 Model model) {
        List<Booking> bookings = bookingRepository.findAll();

        if (status != null) {
            bookings = bookings.stream()
                    .filter(b -> b.getBookingStatus() == status)
                    .toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            bookings = bookings.stream()
                    .filter(b -> b.getId().toString().contains(q) ||
                                 b.getUser().getFullName().toLowerCase().contains(q) ||
                                 b.getRoom().getRoomNumber().toLowerCase().contains(q))
                    .toList();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search", search);

        return "manage-bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelBooking(id, null, true);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " was cancelled by Administrator.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}
