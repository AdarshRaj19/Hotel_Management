package com.hotel.booking.ds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Hash Table Unit Tests")
class CustomHashTableTest {

    private CustomHashTable<String, Integer> hashTable;

    @BeforeEach
    void setUp() {
        hashTable = new CustomHashTable<>(4, 0.75f);
    }

    @Test
    @DisplayName("Should put and retrieve key-value pairs correctly")
    void testPutAndGet() {
        hashTable.put("Room101", 1500);
        hashTable.put("Room201", 2500);
        hashTable.put("Room301", 4500);

        assertEquals(3, hashTable.size());
        assertEquals(1500, hashTable.get("Room101"));
        assertEquals(2500, hashTable.get("Room201"));
        assertEquals(4500, hashTable.get("Room301"));
    }

    @Test
    @DisplayName("Should overwrite existing value when same key is inserted")
    void testUpdateExistingKey() {
        hashTable.put("Room101", 1500);
        assertEquals(1500, hashTable.get("Room101"));

        hashTable.put("Room101", 1800);
        assertEquals(1800, hashTable.get("Room101"));
        assertEquals(1, hashTable.size());
    }

    @Test
    @DisplayName("Should remove key-value mapping accurately")
    void testRemove() {
        hashTable.put("Room101", 1500);
        hashTable.put("Room102", 1500);

        Integer removed = hashTable.remove("Room101");
        assertEquals(1500, removed);
        assertNull(hashTable.get("Room101"));
        assertEquals(1, hashTable.size());

        assertNull(hashTable.remove("NonExistentKey"));
    }

    @Test
    @DisplayName("Should verify containsKey correctly")
    void testContainsKey() {
        hashTable.put("Room501", 8000);

        assertTrue(hashTable.containsKey("Room501"));
        assertFalse(hashTable.containsKey("Room999"));
    }

    @Test
    @DisplayName("Should dynamically resize and rehash when capacity threshold is met")
    void testDynamicResizing() {
        // Initial capacity is 4, threshold is 4 * 0.75 = 3
        for (int i = 1; i <= 20; i++) {
            hashTable.put("Room" + i, i * 100);
        }

        assertEquals(20, hashTable.size());
        for (int i = 1; i <= 20; i++) {
            assertEquals(i * 100, hashTable.get("Room" + i));
        }
    }
}
