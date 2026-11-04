package com.hotel.booking.controller;

import com.hotel.booking.dto.OccupancyReportResponse;
import com.hotel.booking.dto.RevenueReportResponse;
import com.hotel.booking.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String viewReports(Model model) {
        OccupancyReportResponse occupancy = reportService.getOccupancyReport();
        RevenueReportResponse revenue = reportService.getRevenueReport();

        model.addAttribute("occupancy", occupancy);
        model.addAttribute("revenue", revenue);

        return "reports";
    }
}
