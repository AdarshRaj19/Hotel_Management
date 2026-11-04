package com.hotel.booking.controller;

import com.hotel.booking.dto.RoomSearchCriteria;
import com.hotel.booking.entity.Room;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.service.RoomTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    public RoomController(RoomService roomService, RoomTypeService roomTypeService) {
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public String listRooms(@ModelAttribute("criteria") RoomSearchCriteria criteria, Model model) {
        List<Room> rooms = roomService.searchRooms(criteria);
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypeService.getAllRoomTypes());
        model.addAttribute("criteria", criteria);
        return "rooms";
    }

    @GetMapping("/{id}")
    public String roomDetails(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id);
        model.addAttribute("room", room);
        return "room-details";
    }
}
