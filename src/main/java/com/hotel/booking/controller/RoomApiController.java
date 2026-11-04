package com.hotel.booking.controller;

import com.hotel.booking.dto.RoomRequest;
import com.hotel.booking.dto.RoomResponse;
import com.hotel.booking.entity.Room;
import com.hotel.booking.service.RoomCacheService;
import com.hotel.booking.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {

    private final RoomService roomService;
    private final RoomCacheService roomCacheService;

    public RoomApiController(RoomService roomService, RoomCacheService roomCacheService) {
        this.roomService = roomService;
        this.roomCacheService = roomCacheService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRoomResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomResponseById(id));
    }

    @GetMapping("/lookup/{roomNumber}")
    public ResponseEntity<RoomResponse> getRoomByNumberFast(@PathVariable String roomNumber) {
        Room room = roomCacheService.getRoomByNumber(roomNumber);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(RoomResponse.fromEntity(room));
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms() {
        List<RoomResponse> available = roomService.getAvailableRooms().stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(available);
    }

    @PostMapping
    public ResponseEntity<RoomResponse> addRoom(@Valid @RequestBody RoomRequest request) {
        Room created = roomService.addRoom(request);
        roomCacheService.cacheRoom(created);
        return new ResponseEntity<>(RoomResponse.fromEntity(created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        Room updated = roomService.updateRoom(id, request);
        roomCacheService.cacheRoom(updated);
        return ResponseEntity.ok(RoomResponse.fromEntity(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        roomCacheService.evictRoom(room.getRoomNumber());
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}
