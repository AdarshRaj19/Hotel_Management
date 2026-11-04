package com.hotel.booking.controller;

import com.hotel.booking.dto.RoomResponse;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.RoomType;
import com.hotel.booking.exception.ResourceNotFoundException;
import com.hotel.booking.service.RoomCacheService;
import com.hotel.booking.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoomApiController.
 * Tests are pure Mockito unit tests (no @WebMvcTest — not available in Spring Boot 4).
 * Controller methods are called directly; HTTP status codes verified via ResponseEntity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoomApiController Unit Tests")
class RoomApiControllerTest {

    @Mock private RoomService roomService;
    @Mock private RoomCacheService roomCacheService;

    @InjectMocks
    private RoomApiController controller;

    private RoomType standardType;
    private Room room101;

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
    }

    private RoomResponse buildResponse(Long id, String number, String typeName, Double price) {
        RoomResponse r = new RoomResponse();
        r.setId(id);
        r.setRoomNumber(number);
        r.setRoomTypeName(typeName);
        r.setStatus(RoomStatus.AVAILABLE);
        r.setPricePerNight(price);
        return r;
    }

    // ─── GET /api/rooms ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllRooms: returns 200 with list of room responses")
    void getAllRooms_returns200WithList() {
        List<RoomResponse> rooms = List.of(
                buildResponse(1L, "101", "Standard", 3000.0),
                buildResponse(2L, "201", "Deluxe",   5000.0)
        );
        when(roomService.getAllRoomResponses()).thenReturn(rooms);

        ResponseEntity<List<RoomResponse>> response = controller.getAllRooms();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getRoomNumber()).isEqualTo("101");
        assertThat(response.getBody().get(1).getRoomNumber()).isEqualTo("201");
    }

    @Test
    @DisplayName("getAllRooms: empty list returns 200 with empty body")
    void getAllRooms_emptyList_returns200() {
        when(roomService.getAllRoomResponses()).thenReturn(List.of());

        ResponseEntity<List<RoomResponse>> response = controller.getAllRooms();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ─── GET /api/rooms/{id} ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getRoomById: found room returns 200 with data")
    void getRoomById_found_returns200() {
        RoomResponse resp = buildResponse(1L, "101", "Standard", 3000.0);
        when(roomService.getRoomResponseById(1L)).thenReturn(resp);

        ResponseEntity<RoomResponse> response = controller.getRoomById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1L);
        assertThat(response.getBody().getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("getRoomById: missing room propagates ResourceNotFoundException")
    void getRoomById_notFound_throwsException() {
        when(roomService.getRoomResponseById(999L))
                .thenThrow(new ResourceNotFoundException("Room not found with id: 999"));

        assertThatThrownBy(() -> controller.getRoomById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ─── GET /api/rooms/lookup/{roomNumber} ──────────────────────────────────────

    @Test
    @DisplayName("getRoomByNumberFast: cached room returns 200 with response")
    void lookupByRoomNumber_found_returns200() {
        when(roomCacheService.getRoomByNumber("101")).thenReturn(room101);

        ResponseEntity<RoomResponse> response = controller.getRoomByNumberFast("101");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("getRoomByNumberFast: uncached room number returns 404")
    void lookupByRoomNumber_notFound_returns404() {
        when(roomCacheService.getRoomByNumber("999")).thenReturn(null);

        ResponseEntity<RoomResponse> response = controller.getRoomByNumberFast("999");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    // ─── GET /api/rooms/available ────────────────────────────────────────────────

    @Test
    @DisplayName("getAvailableRooms: returns 200 with only available rooms")
    void getAvailableRooms_returns200() {
        when(roomService.getAvailableRooms()).thenReturn(List.of(room101));

        ResponseEntity<List<RoomResponse>> response = controller.getAvailableRooms();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getRoomNumber()).isEqualTo("101");
    }

    // ─── POST /api/rooms ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("addRoom: successfully added room returns 201 CREATED")
    void addRoom_success_returns201() {
        com.hotel.booking.dto.RoomRequest req = new com.hotel.booking.dto.RoomRequest();
        req.setRoomNumber("102");
        req.setRoomTypeId(1L);
        req.setCapacity(2);
        req.setPricePerNight(3000.0);
        req.setDescription("Test");

        Room saved = new Room();
        saved.setId(2L);
        saved.setRoomNumber("102");
        saved.setRoomType(standardType);
        saved.setStatus(RoomStatus.AVAILABLE);
        saved.setCapacity(2);
        saved.setPricePerNight(3000.0);

        when(roomService.addRoom(req)).thenReturn(saved);

        ResponseEntity<RoomResponse> response = controller.addRoom(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRoomNumber()).isEqualTo("102");
        verify(roomCacheService).cacheRoom(saved);
    }
}
