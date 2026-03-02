package com.example.grocery.dto.common;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> items;

    private int page;          // หน้าปัจจุบัน (0-based)
    private int size;          // ขนาดต่อหน้า
    private long totalItems;   // จำนวนทั้งหมด
    private int totalPages;    // จำนวนหน้า
    private boolean hasNext;
    private boolean hasPrev;
}
