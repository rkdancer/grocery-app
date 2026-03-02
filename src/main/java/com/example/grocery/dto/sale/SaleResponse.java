package com.example.grocery.dto.sale;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaleResponse {
    private boolean success;
    private String message;
    private Long saleId;
    private Double totalAmount;
}
