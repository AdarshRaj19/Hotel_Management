package com.hotel.booking.controller;

import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.User;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final UserService userService;
    private final BookingRepository bookingRepository;

    public CustomerController(UserService userService, BookingRepository bookingRepository) {
        this.userService = userService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User user = userService.getUserByEmail(userDetails.getUsername());
            List<Booking> bookings = bookingRepository.findByUserId(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("bookings", bookings);
            model.addAttribute("totalBookings", bookings.size());
        }
        return "customer-dashboard";
    }
}
