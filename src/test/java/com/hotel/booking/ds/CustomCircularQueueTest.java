package com.hotel.booking.ds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Custom Circular Queue Unit Tests")
class CustomCircularQueueTest {

    private CustomCircularQueue<String> queue;

    @BeforeEach
    void setUp() {
        queue = new CustomCircularQueue<>(3);
    }

    @Test
    @DisplayName("Should enqueue and dequeue in FIFO order")
    void testEnqueueAndDequeueFIFO() {
        queue.enqueue("Customer 1");
        queue.enqueue("Customer 2");
        queue.enqueue("Customer 3");

        assertEquals(3, queue.size());
        assertTrue(queue.isFull());

        assertEquals("Customer 1", queue.dequeue());
        assertEquals("Customer 2", queue.dequeue());
        assertEquals("Customer 3", queue.dequeue());

        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("Should correctly peek front element without removing")
    void testPeek() {
        assertNull(queue.peek());

        queue.enqueue("Guest A");
        assertEquals("Guest A", queue.peek());
        assertEquals(1, queue.size());

        queue.enqueue("Guest B");
        assertEquals("Guest A", queue.peek());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when enqueuing to full queue")
    void testEnqueueOverflow() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> queue.enqueue("D"));
        assertTrue(ex.getMessage().contains("Queue is full"));
    }

    @Test
    @DisplayName("Should throw NoSuchElementException when dequeuing from empty queue")
    void testDequeueUnderflow() {
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> queue.dequeue());
        assertTrue(ex.getMessage().contains("Queue is empty"));
    }

    @Test
    @DisplayName("Should handle circular wrap-around index correctly")
    void testCircularWrapAround() {
        queue.enqueue("First");
        queue.enqueue("Second");
        assertEquals("First", queue.dequeue());

        // Now front is 1, rear is 1. Enqueue two more to trigger modulo wrap-around
        queue.enqueue("Third");
        queue.enqueue("Fourth"); // rear wraps around to 0

        assertTrue(queue.isFull());
        assertEquals("Second", queue.dequeue());
        assertEquals("Third", queue.dequeue());
        assertEquals("Fourth", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("Should convert to ordered List representation correctly")
    void testToList() {
        queue.enqueue("Alpha");
        queue.enqueue("Beta");

        List<String> list = queue.toList();
        assertEquals(2, list.size());
        assertEquals("Alpha", list.get(0));
        assertEquals("Beta", list.get(1));
    }
}
