package com.hotel.booking.dto;

import java.util.Map;

public class OccupancyReportResponse {

    private long totalRooms;
    private long availableRooms;
    private long occupiedRooms;
    private long maintenanceRooms;
    private double occupancyRate;
    private Map<String, Long> roomsByRoomType;

    public OccupancyReportResponse() {
    }

    public OccupancyReportResponse(long totalRooms, long availableRooms, long occupiedRooms, long maintenanceRooms, double occupancyRate, Map<String, Long> roomsByRoomType) {
        this.totalRooms = totalRooms;
        this.availableRooms = availableRooms;
        this.occupiedRooms = occupiedRooms;
        this.maintenanceRooms = maintenanceRooms;
        this.occupancyRate = occupancyRate;
        this.roomsByRoomType = roomsByRoomType;
    }

    // Getters and Setters
    public long getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(long totalRooms) {
        this.totalRooms = totalRooms;
    }

    public long getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(long availableRooms) {
        this.availableRooms = availableRooms;
    }

    public long getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(long occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public long getMaintenanceRooms() {
        return maintenanceRooms;
    }

    public void setMaintenanceRooms(long maintenanceRooms) {
        this.maintenanceRooms = maintenanceRooms;
    }

    public double getOccupancyRate() {
        return occupancyRate;
    }

    public void setOccupancyRate(double occupancyRate) {
        this.occupancyRate = occupancyRate;
    }

    public Map<String, Long> getRoomsByRoomType() {
        return roomsByRoomType;
    }

    public void setRoomsByRoomType(Map<String, Long> roomsByRoomType) {
        this.roomsByRoomType = roomsByRoomType;
    }
}
