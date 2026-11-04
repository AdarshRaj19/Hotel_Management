package com.hotel.booking.service;

import com.hotel.booking.dto.RoomRequest;
import com.hotel.booking.dto.RoomSearchCriteria;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.RoomType;
import com.hotel.booking.exception.DuplicateResourceException;
import com.hotel.booking.exception.ResourceNotFoundException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.RoomTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Unit Tests")
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks
    private RoomService roomService;

    // RoomType only has: id, name, description, defaultPrice (no capacity)
    private RoomType standardType;
    private Room room101;
    private Room room201;

    @BeforeEach
    void setUp() {
        standardType = new RoomType("Standard", "Standard room", 3000.0);
        standardType.setId(1L);

        room101 = new Room();
        room101.setId(1L);
        room101.setRoomNumber("101");
        room101.setRoomType(standardType);
        room101.setStatus(RoomStatus.AVAILABLE);
        room101.setCapacity(2);
        room101.setPricePerNight(3000.0);
        room101.setDescription("Cozy standard room");

        room201 = new Room();
        room201.setId(2L);
        room201.setRoomNumber("201");
        room201.setRoomType(standardType);
        room201.setStatus(RoomStatus.AVAILABLE);
        room201.setCapacity(2);
        room201.setPricePerNight(4500.0);
        room201.setDescription("Deluxe room");
    }

    // ─── addRoom ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addRoom: success — returns saved Room entity")
    void addRoom_success() {
        RoomRequest req = buildRoomRequest("102");
        Room saved = new Room();
        saved.setId(3L);
        saved.setRoomNumber("102");
        saved.setRoomType(standardType);
        saved.setStatus(RoomStatus.AVAILABLE);
        saved.setCapacity(2);
        saved.setPricePerNight(3000.0);

        when(roomRepository.existsByRoomNumber("102")).thenReturn(false);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(standardType));
        when(roomRepository.save(any(Room.class))).thenReturn(saved);

        Room result = roomService.addRoom(req);

        assertThat(result).isNotNull();
        assertThat(result.getRoomNumber()).isEqualTo("102");
        assertThat(result.getId()).isEqualTo(3L);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("addRoom: duplicate room number throws DuplicateResourceException")
    void addRoom_duplicateRoomNumber_throws() {
        RoomRequest req = buildRoomRequest("101");
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        assertThatThrownBy(() -> roomService.addRoom(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("101");

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("addRoom: unknown room type throws ResourceNotFoundException")
    void addRoom_unknownRoomType_throws() {
        RoomRequest req = buildRoomRequest("103");
        when(roomRepository.existsByRoomNumber("103")).thenReturn(false);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.addRoom(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room type");
    }

    // ─── searchRooms ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchRooms: no criteria returns all AVAILABLE rooms")
    void searchRooms_noCriteria_returnsAll() {
        when(roomRepository.findAll()).thenReturn(List.of(room101, room201));

        List<Room> results = roomService.searchRooms(new RoomSearchCriteria());

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("searchRooms: OCCUPIED rooms are excluded")
    void searchRooms_excludesOccupiedRooms() {
        room201.setStatus(RoomStatus.OCCUPIED);
        when(roomRepository.findAll()).thenReturn(List.of(room101, room201));

        List<Room> results = roomService.searchRooms(new RoomSearchCriteria());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("searchRooms: filters by maxPrice (Double)")
    void searchRooms_filterByMaxPrice() {
        Room expensive = new Room();
        expensive.setId(3L);
        expensive.setRoomNumber("301");
        expensive.setRoomType(standardType);
        expensive.setStatus(RoomStatus.AVAILABLE);
        expensive.setCapacity(4);
        expensive.setPricePerNight(10000.0);

        when(roomRepository.findAll()).thenReturn(List.of(room101, expensive));

        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setMaxPrice(5000.0);

        List<Room> results = roomService.searchRooms(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("searchRooms: filters by roomTypeId")
    void searchRooms_filterByRoomTypeId() {
        RoomType suiteType = new RoomType("Suite", "Luxury suite", 8000.0);
        suiteType.setId(2L);

        Room suite = new Room();
        suite.setId(4L);
        suite.setRoomNumber("401");
        suite.setRoomType(suiteType);
        suite.setStatus(RoomStatus.AVAILABLE);
        suite.setCapacity(4);
        suite.setPricePerNight(8000.0);

        when(roomRepository.findAll()).thenReturn(List.of(room101, suite));

        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setRoomTypeId(2L);

        List<Room> results = roomService.searchRooms(criteria);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomNumber()).isEqualTo("401");
    }

    // ─── getRoomById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRoomById: existing id returns Room entity")
    void getRoomById_found() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room101));

        Room result = roomService.getRoomById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("getRoomById: missing id throws ResourceNotFoundException")
    void getRoomById_notFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private RoomRequest buildRoomRequest(String number) {
        RoomRequest req = new RoomRequest();
        req.setRoomNumber(number);
        req.setRoomTypeId(1L);
        req.setCapacity(2);
        req.setPricePerNight(3000.0);
        req.setDescription("Test room");
        return req;
    }
}
