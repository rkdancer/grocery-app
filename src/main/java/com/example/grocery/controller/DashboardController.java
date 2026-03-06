package com.example.grocery.controller;

import com.example.grocery.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public Map<String, Object> summary(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.summary(userId);
    }

    @GetMapping("/low-stock")
    public Object lowStock(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.lowStockProducts(userId);
    }

    @GetMapping("/top-selling")
    public Object topSelling(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.topSellingProducts(userId);
    }

    @GetMapping("/recent-sales")
    public Object recentSales(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.recentSales(userId);
    }

    @GetMapping("/audit-logs")
    public Object auditLogs(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.recentAuditLogs(userId);
    }

    @GetMapping("/sales-daily")
    public Object salesDaily(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days
    ) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.salesDaily(userId, days);
    }

    @GetMapping("/sales-daily-stacked")
    public Object salesDailyStacked(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "5") int top,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "amount") String metric
    ) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.salesDailyStacked(userId, days, top, categoryId, metric);
    }

    @GetMapping("/sales-monthly")
    public Object salesMonthly(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("authUserId");
        return dashboardService.salesMonthly(userId);
    }
}