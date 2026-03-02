package com.example.grocery.controller;

import com.example.grocery.dto.category.CategoryResponse;
import com.example.grocery.dto.common.PageResponse;
import com.example.grocery.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // =========================
    // LIST (pagination + search)
    // =========================
    @GetMapping("/api/categories")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String q
    ) {
        try {
            PageResponse<CategoryResponse> resp =
                    categoryService.list(page, size, q);
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
            value = "/api/categories",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> create(
            HttpServletRequest request,
            @RequestParam("name") String name,
            @RequestPart("image") MultipartFile image
    ) {
        try {
            Long authUserId = (Long) request.getAttribute("authUserId");

            CategoryResponse resp =
                    categoryService.create(authUserId, name, image);

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
            value = "/api/categories/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            Long authUserId = (Long) request.getAttribute("authUserId");

            CategoryResponse resp =
                    categoryService.update(authUserId, id, name, image);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/api/categories/{id}")
    public ResponseEntity<?> delete(
            HttpServletRequest request,
            @PathVariable Long id
    ) {
        try {
            Long authUserId = (Long) request.getAttribute("authUserId");

            categoryService.delete(authUserId, id);

            return ResponseEntity.ok(
                    Map.of("success", true, "message", "ลบประเภทสินค้าเรียบร้อย")
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
