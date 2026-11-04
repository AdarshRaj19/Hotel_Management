package com.hotel.booking.service;

import com.hotel.booking.dto.OccupancyReportResponse;
import com.hotel.booking.dto.RevenueReportResponse;
import com.hotel.booking.entity.*;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.PaymentRepository;
import com.hotel.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public ReportService(RoomRepository roomRepository,
                         BookingRepository bookingRepository,
                         PaymentRepository paymentRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    public OccupancyReportResponse getOccupancyReport() {
        List<Room> allRooms = roomRepository.findAll();
        long totalRooms = allRooms.size();

        long availableRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .count();

        long occupiedRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED)
                .count();

        long maintenanceRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.MAINTENANCE)
                .count();

        double occupancyRate = totalRooms > 0
                ? Math.round(((double) occupiedRooms / totalRooms) * 100.0 * 10.0) / 10.0
                : 0.0;

        // Demonstrate Java Streams groupingBy
        Map<String, Long> roomsByRoomType = allRooms.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getRoomType().getName(),
                        Collectors.counting()
                ));

        return new OccupancyReportResponse(totalRooms, availableRooms, occupiedRooms, maintenanceRooms, occupancyRate, roomsByRoomType);
    }

    public RevenueReportResponse getRevenueReport() {
        List<Payment> completedPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.COMPLETED)
                .toList();

        // Demonstrate Stream API summingDouble & method reference
        double totalRevenue = completedPayments.stream()
                .mapToDouble(Payment::getAmount)
                .sum();

        long totalTransactions = completedPayments.size();
        double avgTransaction = totalTransactions > 0 ? (totalRevenue / totalTransactions) : 0.0;

        // Group revenue by payment method
        Map<String, Double> revenueByPaymentMethod = completedPayments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod().name(),
                        Collectors.summingDouble(Payment::getAmount)
                ));

        // Group revenue by room type
        Map<String, Double> revenueByRoomType = completedPayments.stream()
                .filter(p -> p.getBooking() != null && p.getBooking().getRoom() != null && p.getBooking().getRoom().getRoomType() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getBooking().getRoom().getRoomType().getName(),
                        Collectors.summingDouble(Payment::getAmount)
                ));

        return new RevenueReportResponse(
                Math.round(totalRevenue * 100.0) / 100.0,
                totalTransactions,
                Math.round(avgTransaction * 100.0) / 100.0,
                revenueByPaymentMethod,
                revenueByRoomType
        );
    }
}
