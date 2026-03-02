package com.example.grocery.dto.product;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private Long categoryId;
    private String name;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Integer stockQty;
    private Integer monthlySales;
    private String imageUrl;
}
