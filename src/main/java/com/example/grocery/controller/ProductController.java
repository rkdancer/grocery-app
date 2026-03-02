package com.example.grocery.controller;

import com.example.grocery.dto.common.PageResponse;
import com.example.grocery.dto.product.ProductResponse;
import com.example.grocery.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // =========================
    // LIST (pagination + search)
    // =========================
    @GetMapping("/api/categories/{categoryId}/products")
    public ResponseEntity<?> listByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String q
    ) {
        try {
            PageResponse<ProductResponse> resp =
                    productService.listByCategory(categoryId, page, size, q);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================
    // CREATE
    // =========================
    @PostMapping(
            value = "/api/categories/{categoryId}/products",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> create(
            @PathVariable Long categoryId,

            @RequestParam("name") String name,
            @RequestParam("buyPrice") BigDecimal buyPrice,
            @RequestParam("sellPrice") BigDecimal sellPrice,
            @RequestParam("stockQty") Integer stockQty,
            @RequestParam(value = "monthlySales", required = false) Integer monthlySales,

            @RequestPart("image") MultipartFile image,
            HttpServletRequest request
    ) {
        try {
            Long userId = (Long) request.getAttribute("authUserId");

            ProductResponse resp = productService.create(
                    categoryId,
                    name,
                    buyPrice,
                    sellPrice,
                    stockQty,
                    monthlySales,
                    image,
                    userId
            );
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping(
            value = "/api/products/{productId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> update(
            @PathVariable Long productId,

            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "buyPrice", required = false) BigDecimal buyPrice,
            @RequestParam(value = "sellPrice", required = false) BigDecimal sellPrice,
            @RequestParam(value = "stockQty", required = false) Integer stockQty,
            @RequestParam(value = "monthlySales", required = false) Integer monthlySales,

            @RequestPart(value = "image", required = false) MultipartFile image,
            HttpServletRequest request
    ) {
        try {
            Long userId = (Long) request.getAttribute("authUserId");

            ProductResponse resp = productService.update(
                    productId,
                    name,
                    buyPrice,
                    sellPrice,
                    stockQty,
                    monthlySales,
                    image,
                    userId
            );
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/api/products/{productId}")
    public ResponseEntity<?> delete(
            @PathVariable Long productId,
            HttpServletRequest request
    ) {
        try {
            Long userId = (Long) request.getAttribute("authUserId");

            productService.delete(productId, userId);
            return ResponseEntity.ok(
                    Map.of("success", true, "message", "ลบสินค้าเรียบร้อย")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
