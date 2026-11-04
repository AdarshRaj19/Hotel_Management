package com.hotel.booking.config;

import com.hotel.booking.entity.Role;
import com.hotel.booking.entity.Room;
import com.hotel.booking.entity.RoomStatus;
import com.hotel.booking.entity.RoomType;
import com.hotel.booking.entity.User;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.RoomTypeRepository;
import com.hotel.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoomTypeRepository roomTypeRepository,
                           RoomRepository roomRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedRoomTypesAndRooms();
        seedUsers();
    }

    private void seedRoomTypesAndRooms() {
        if (roomTypeRepository.count() == 0) {
            RoomType single = roomTypeRepository.save(new RoomType("Single Room", "Cozy single room with high-speed Wi-Fi, work desk, and ensuite bathroom.", 1500.0));
            RoomType doubleRoom = roomTypeRepository.save(new RoomType("Double Room", "Spacious room with king bed, flat-screen TV, minibar, and city views.", 2500.0));
            RoomType deluxe = roomTypeRepository.save(new RoomType("Deluxe Suite", "Luxurious room with lounge seating, jacuzzi, panoramic balcony, and premium amenities.", 4500.0));
            RoomType presidential = roomTypeRepository.save(new RoomType("Presidential Suite", "Ultimate luxury suite with separate living room, dining area, butler service, and private bar.", 8000.0));

            if (roomRepository.count() == 0) {
                roomRepository.save(new Room("101", single, 1500.0, 1, "Ground floor quiet single room facing the garden.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("102", single, 1500.0, 1, "Comfortable single room near the lounge.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("201", doubleRoom, 2500.0, 2, "Second floor double room with a queen bed and balcony.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("202", doubleRoom, 2500.0, 2, "Quiet corner double room with scenic mountain views.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("301", deluxe, 4500.0, 3, "Third floor deluxe suite with king bed and extra sofa bed.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("302", deluxe, 4500.0, 3, "Deluxe family suite with complimentary breakfast and lounge access.", RoomStatus.AVAILABLE));
                roomRepository.save(new Room("401", presidential, 8000.0, 4, "Top floor presidential suite with private terrace and hot tub.", RoomStatus.AVAILABLE));
            }
        }
    }

    private void seedUsers() {
        // Ensure default accounts exist with BCrypt encrypted passwords
        upsertUser("System Administrator", "admin@hotelease.com", "admin123", "+91 9876543210", Role.ROLE_ADMIN);
        upsertUser("Front Desk Reception", "reception@hotelease.com", "reception123", "+91 9876543211", Role.ROLE_RECEPTION);
        upsertUser("Rahul Sharma", "customer@hotelease.com", "customer123", "+91 9876543212", Role.ROLE_CUSTOMER);
    }

    private void upsertUser(String fullName, String email, String rawPassword, String phone, Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    // Update password with BCrypt if needed
                    if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
                        user.setPassword(passwordEncoder.encode(rawPassword));
                        userRepository.save(user);
                    }
                },
                () -> {
                    User newUser = new User(fullName, email, passwordEncoder.encode(rawPassword), phone, role);
                    newUser.setCreatedAt(LocalDateTime.now());
                    userRepository.save(newUser);
                }
        );
    }
}
