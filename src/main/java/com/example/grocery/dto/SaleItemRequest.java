package com.example.grocery.dto;

import lombok.Data;

@Data
public class SaleItemRequest {
    private Long productId;
    private Integer qty;
}
