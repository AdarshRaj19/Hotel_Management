package com.hotel.booking.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Custom Hash Table implementation with separate chaining collision resolution.
 * Developed from scratch for HCL Java assignment to provide fast O(1) room lookups by room number.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class CustomHashTable<K, V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private Node<K, V>[] buckets;
    private int capacity;
    private int size;
    private final float loadFactor;

    static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    @SuppressWarnings("unchecked")
    public CustomHashTable(int initialCapacity, float loadFactor) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = initialCapacity;
        this.loadFactor = loadFactor;
        this.buckets = new Node[capacity];
        this.size = 0;
    }

    public CustomHashTable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    private int hash(K key) {
        if (key == null) {
            return 0;
        }
        int h = key.hashCode();
        // Spread higher bits to minimize collisions
        h = h ^ (h >>> 16);
        return Math.abs(h % capacity);
    }

    /**
     * Inserts or updates a key-value mapping.
     * Time Complexity: O(1) average
     */
    public synchronized void put(K key, V value) {
        int index = hash(key);
        Node<K, V> head = buckets[index];

        while (head != null) {
            if (Objects.equals(head.key, key)) {
                head.value = value; // Update existing
                return;
            }
            head = head.next;
        }

        // Insert at beginning of bucket chain
        Node<K, V> newNode = new Node<>(key, value, buckets[index]);
        buckets[index] = newNode;
        size++;

        // Resize if load factor exceeded
        if ((float) size / capacity >= loadFactor) {
            resize();
        }
    }

    /**
     * Retrieves the value mapped to the specified key.
     * Time Complexity: O(1) average
     */
    public synchronized V get(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Removes the mapping for the specified key.
     * Time Complexity: O(1) average
     */
    public synchronized V remove(K key) {
        int index = hash(key);
        Node<K, V> current = buckets[index];
        Node<K, V> prev = null;

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                if (prev == null) {
                    buckets[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null;
    }

    public synchronized boolean containsKey(K key) {
        return get(key) != null;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    public synchronized List<V> values() {
        List<V> list = new ArrayList<>(size);
        for (Node<K, V> bucket : buckets) {
            Node<K, V> curr = bucket;
            while (curr != null) {
                list.add(curr.value);
                curr = curr.next;
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Node<K, V>[] newBuckets = new Node[newCapacity];
        int oldCapacity = capacity;
        this.capacity = newCapacity;

        for (int i = 0; i < oldCapacity; i++) {
            Node<K, V> head = buckets[i];
            while (head != null) {
                Node<K, V> next = head.next;
                int newIndex = hash(head.key);
                head.next = newBuckets[newIndex];
                newBuckets[newIndex] = head;
                head = next;
            }
        }
        this.buckets = newBuckets;
    }

    public synchronized void clear() {
        for (int i = 0; i < capacity; i++) {
            buckets[i] = null;
        }
        size = 0;
    }
}
