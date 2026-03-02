package com.example.grocery.repository;

import com.example.grocery.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // เดิม: list ตามหมวด
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // เดิม: list ตามหมวด + ค้นหาชื่อสินค้า (contains ignorecase)
    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name, Pageable pageable);

    // ✅ NEW: กันชื่อสินค้าซ้ำในหมวดเดียวกัน
    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    // ✅ NEW: กันชื่อซ้ำตอนแก้ไข (ยกเว้นตัวเอง)
    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);
}