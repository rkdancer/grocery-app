package com.example.grocery.dto.sale;

import lombok.Data;
import java.util.List;

@Data
public class CreateSaleRequest {

    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private Integer quantity;
    }
}
