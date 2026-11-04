package com.hotel.booking.service;

import com.hotel.booking.ds.CustomHashTable;
import com.hotel.booking.entity.Room;
import com.hotel.booking.repository.RoomRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service providing fast O(1) room lookup by room number using our custom Hash Table data structure.
 */
@Service
public class RoomCacheService {

    private final CustomHashTable<String, Room> roomHashTable;
    private final RoomRepository roomRepository;

    public RoomCacheService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
        this.roomHashTable = new CustomHashTable<>(32, 0.75f);
    }

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    public synchronized void refreshCache() {
        roomHashTable.clear();
        List<Room> rooms = roomRepository.findAll();
        for (Room room : rooms) {
            roomHashTable.put(room.getRoomNumber(), room);
        }
    }

    public Room getRoomByNumber(String roomNumber) {
        if (roomNumber == null) {
            return null;
        }
        Room room = roomHashTable.get(roomNumber);
        if (room == null) {
            // Fallback to database and cache
            roomRepository.findByRoomNumber(roomNumber).ifPresent(r -> roomHashTable.put(r.getRoomNumber(), r));
            return roomHashTable.get(roomNumber);
        }
        return room;
    }

    public void cacheRoom(Room room) {
        if (room != null && room.getRoomNumber() != null) {
            roomHashTable.put(room.getRoomNumber(), room);
        }
    }

    public void evictRoom(String roomNumber) {
        if (roomNumber != null) {
            roomHashTable.remove(roomNumber);
        }
    }

    public int getCachedRoomCount() {
        return roomHashTable.size();
    }
}
