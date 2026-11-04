package com.hotel.booking.controller;

import com.hotel.booking.dto.BookingRequest;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.User;
import com.hotel.booking.service.BookingService;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final UserService userService;

    public BookingController(BookingService bookingService,
                             RoomService roomService,
                             UserService userService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping("/booking/new")
    public String showBookingForm(@RequestParam(value = "roomId", required = false) Long roomId,
                                  Model model) {
        List<Room> availableRooms = roomService.getAvailableRooms();
        model.addAttribute("rooms", availableRooms);

        BookingRequest bookingRequest = new BookingRequest();
        if (roomId != null) {
            bookingRequest.setRoomId(roomId);
            Room selectedRoom = roomService.getRoomById(roomId);
            model.addAttribute("selectedRoom", selectedRoom);
        } else if (!availableRooms.isEmpty()) {
            bookingRequest.setRoomId(availableRooms.get(0).getId());
            model.addAttribute("selectedRoom", availableRooms.get(0));
        }

        model.addAttribute("bookingRequest", bookingRequest);
        return "booking";
    }

    @PostMapping("/booking/create")
    public String createBooking(@Valid @ModelAttribute("bookingRequest") BookingRequest bookingRequest,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", roomService.getAvailableRooms());
            if (bookingRequest.getRoomId() != null) {
                model.addAttribute("selectedRoom", roomService.getRoomById(bookingRequest.getRoomId()));
            }
            return "booking";
        }

        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            Booking created = bookingService.createBooking(user.getId(), bookingRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Reservation confirmed! Booking #" + created.getId());
            return "redirect:/booking/" + created.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("rooms", roomService.getAvailableRooms());
            if (bookingRequest.getRoomId() != null) {
                model.addAttribute("selectedRoom", roomService.getRoomById(bookingRequest.getRoomId()));
            }
            return "booking";
        }
    }

    @GetMapping("/booking/{id}")
    public String viewBooking(@PathVariable Long id, Model model) {
        Booking booking = bookingService.getBookingById(id);
        model.addAttribute("booking", booking);
        return "booking-details";
    }

    @PostMapping("/booking/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserByEmail(userDetails.getUsername());
            boolean isStaff = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTION"));

            bookingService.cancelBooking(id, user.getId(), isStaff);
            redirectAttributes.addFlashAttribute("successMessage", "Booking #" + id + " has been successfully cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/my-bookings";
    }

    @GetMapping("/my-bookings")
    public String myBookings(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getUserByEmail(userDetails.getUsername());
        List<Booking> bookings = bookingService.getBookingsByUserId(user.getId());
        model.addAttribute("bookings", bookings);
        return "my-bookings";
    }
}
