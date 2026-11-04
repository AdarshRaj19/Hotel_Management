package com.hotel.booking.service;

import com.hotel.booking.dto.RoomRequest;
import com.hotel.booking.dto.RoomResponse;
import com.hotel.booking.dto.RoomSearchCriteria;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.RoomType;
import com.hotel.booking.exception.DuplicateResourceException;
import com.hotel.booking.exception.ResourceNotFoundException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRepository bookingRepository;

    public RoomService(RoomRepository roomRepository,
                       RoomTypeRepository roomTypeRepository,
                       BookingRepository bookingRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRoomResponses() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomResponseById(Long id) {
        return RoomResponse.fromEntity(getRoomById(id));
    }

    public Room addRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new DuplicateResourceException("Room number " + request.getRoomNumber() + " already exists");
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with ID: " + request.getRoomTypeId()));

        Room room = new Room(
                request.getRoomNumber(),
                roomType,
                request.getPricePerNight(),
                request.getCapacity(),
                request.getDescription(),
                request.getStatus() != null ? request.getStatus() : RoomStatus.AVAILABLE
        );

        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, RoomRequest request) {
        Room room = getRoomById(id);

        if (!room.getRoomNumber().equalsIgnoreCase(request.getRoomNumber()) &&
                roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new DuplicateResourceException("Room number " + request.getRoomNumber() + " already exists");
        }

        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with ID: " + request.getRoomTypeId()));

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(roomType);
        room.setPricePerNight(request.getPricePerNight());
        room.setCapacity(request.getCapacity());
        room.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            room.setStatus(request.getStatus());
        }

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms() {
        // Demonstrate Java 8+ Stream API filtering
        return roomRepository.findAll().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Room> searchRooms(RoomSearchCriteria criteria) {
        List<Room> allRooms = roomRepository.findAll();

        return allRooms.stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
                .filter(room -> criteria.getRoomTypeId() == null || room.getRoomType().getId().equals(criteria.getRoomTypeId()))
                .filter(room -> criteria.getCapacity() == null || room.getCapacity() >= criteria.getCapacity())
                .filter(room -> criteria.getMinPrice() == null || room.getPricePerNight() >= criteria.getMinPrice())
                .filter(room -> criteria.getMaxPrice() == null || room.getPricePerNight() <= criteria.getMaxPrice())
                .filter(room -> {
                    if (criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null) {
                        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                                room.getId(),
                                criteria.getCheckInDate(),
                                criteria.getCheckOutDate()
                        );
                        return overlapping.isEmpty();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
