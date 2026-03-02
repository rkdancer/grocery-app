package com.example.grocery.controller;

import com.example.grocery.service.DemoSeedService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    private final DemoSeedService demoSeedService;

    /**
     * Seed ข้อมูลยอดขายย้อนหลังสำหรับพรีเซนต์
     * ตัวอย่าง:
     * POST /api/demo/seed-sales?months=6&minPerMonth=8&maxPerMonth=18&maxItems=4&maxQty=3
     */
    @PostMapping("/seed-sales")
    public Map<String, Object> seedSales(
            HttpServletRequest request,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(defaultValue = "8") int minPerMonth,
            @RequestParam(defaultValue = "18") int maxPerMonth,
            @RequestParam(defaultValue = "4") int maxItems,
            @RequestParam(defaultValue = "3") int maxQty
    ) {
        Long userId = (Long) request.getAttribute("authUserId");
        return demoSeedService.seedSales(userId, months, minPerMonth, maxPerMonth, maxItems, maxQty);
    }
}
