package com.hotel.booking.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Custom Circular Queue implementation developed from scratch for HCL Java assignment.
 * Used at the hotel reception desk to manage the FIFO check-in waiting queue.
 *
 * @param <T> the type of elements held in this queue
 */
public class CustomCircularQueue<T> {

    private final Object[] elements;
    private final int capacity;
    private int front;
    private int rear;
    private int size;

    public CustomCircularQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be greater than zero");
        }
        this.capacity = capacity;
        this.elements = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    /**
     * Adds an element to the rear of the circular queue.
     * Time Complexity: O(1)
     */
    public synchronized void enqueue(T item) {
        if (isFull()) {
            throw new IllegalStateException("Queue is full! Maximum capacity reached (" + capacity + ").");
        }
        rear = (rear + 1) % capacity;
        elements[rear] = item;
        size++;
    }

    /**
     * Removes and returns the element at the front of the circular queue.
     * Time Complexity: O(1)
     */
    @SuppressWarnings("unchecked")
    public synchronized T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty! No customers waiting.");
        }
        T item = (T) elements[front];
        elements[front] = null; // Help GC
        front = (front + 1) % capacity;
        size--;
        return item;
    }

    /**
     * Retrieves, but does not remove, the element at the front of the queue.
     * Time Complexity: O(1)
     */
    @SuppressWarnings("unchecked")
    public synchronized T peek() {
        if (isEmpty()) {
            return null;
        }
        return (T) elements[front];
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    public synchronized boolean isFull() {
        return size == capacity;
    }

    public synchronized int size() {
        return size;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns an ordered snapshot list of all elements from front to rear.
     */
    @SuppressWarnings("unchecked")
    public synchronized List<T> toList() {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            list.add((T) elements[index]);
        }
        return list;
    }

    public synchronized void clear() {
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            elements[index] = null;
        }
        front = 0;
        rear = -1;
        size = 0;
    }
}
