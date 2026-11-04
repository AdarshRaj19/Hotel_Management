package com.hotel.booking.dto;

import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;

public class RoomResponse {

    private Long id;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private Double pricePerNight;
    private Integer capacity;
    private String description;
    private RoomStatus status;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String roomNumber, Long roomTypeId, String roomTypeName, Double pricePerNight, Integer capacity, String description, RoomStatus status) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomTypeId = roomTypeId;
        this.roomTypeName = roomTypeName;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.description = description;
        this.status = status;
    }

    public static RoomResponse fromEntity(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getRoomType() != null ? room.getRoomType().getId() : null,
                room.getRoomType() != null ? room.getRoomType().getName() : null,
                room.getPricePerNight(),
                room.getCapacity(),
                room.getDescription(),
                room.getStatus()
        );
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
