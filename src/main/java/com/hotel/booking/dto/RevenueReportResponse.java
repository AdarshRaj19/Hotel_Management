package com.hotel.booking.dto;

import java.util.Map;

public class RevenueReportResponse {

    private double totalRevenue;
    private long totalTransactions;
    private double averageTransactionValue;
    private Map<String, Double> revenueByPaymentMethod;
    private Map<String, Double> revenueByRoomType;

    public RevenueReportResponse() {
    }

    public RevenueReportResponse(double totalRevenue, long totalTransactions, double averageTransactionValue, Map<String, Double> revenueByPaymentMethod, Map<String, Double> revenueByRoomType) {
        this.totalRevenue = totalRevenue;
        this.totalTransactions = totalTransactions;
        this.averageTransactionValue = averageTransactionValue;
        this.revenueByPaymentMethod = revenueByPaymentMethod;
        this.revenueByRoomType = revenueByRoomType;
    }

    // Getters and Setters
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public double getAverageTransactionValue() {
        return averageTransactionValue;
    }

    public void setAverageTransactionValue(double averageTransactionValue) {
        this.averageTransactionValue = averageTransactionValue;
    }

    public Map<String, Double> getRevenueByPaymentMethod() {
        return revenueByPaymentMethod;
    }

    public void setRevenueByPaymentMethod(Map<String, Double> revenueByPaymentMethod) {
        this.revenueByPaymentMethod = revenueByPaymentMethod;
    }

    public Map<String, Double> getRevenueByRoomType() {
        return revenueByRoomType;
    }

    public void setRevenueByRoomType(Map<String, Double> revenueByRoomType) {
        this.revenueByRoomType = revenueByRoomType;
    }
}
