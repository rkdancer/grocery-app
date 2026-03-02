package com.example.grocery.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SaleRequest {
    private List<SaleItemRequest> items;

    // ✅ DEMO: optional (ใช้ส่งเวลาย้อนหลัง)
    private Instant soldAt;
}
