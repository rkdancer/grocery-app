package com.example.grocery.controller;

import com.example.grocery.dto.SaleRequest;
import com.example.grocery.entity.Sale;
import com.example.grocery.service.SaleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public Sale sell(
            @RequestBody SaleRequest req,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("authUserId");
        return saleService.sell(userId, req);
    }
}
