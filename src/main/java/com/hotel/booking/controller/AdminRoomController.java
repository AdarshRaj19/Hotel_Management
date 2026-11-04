package com.hotel.booking.controller;

import com.hotel.booking.dto.RoomRequest;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public AdminRoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String manageRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        model.addAttribute("statuses", RoomStatus.values());
        if (!model.containsAttribute("roomRequest")) {
            model.addAttribute("roomRequest", new RoomRequest());
        }
        return "manage-rooms";
    }

    @PostMapping("/add")
    public String addRoom(@Valid @ModelAttribute("roomRequest") RoomRequest roomRequest,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("rooms", roomService.getAllRooms());
            model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
            model.addAttribute("statuses", RoomStatus.values());
            return "manage-rooms";
        }
        try {
            roomService.addRoom(roomRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Room " + roomRequest.getRoomNumber() + " added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/edit/{id}")
    public String editRoom(@PathVariable Long id,
                           @Valid @ModelAttribute("roomRequest") RoomRequest roomRequest,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid input for room update.");
            return "redirect:/admin/rooms";
        }
        try {
            roomService.updateRoom(id, roomRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Room updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    @PostMapping("/delete/{id}")
    public String deleteRoom(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Room deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/rooms";
    }
}
