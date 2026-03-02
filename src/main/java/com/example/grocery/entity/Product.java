package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // สินค้าอยู่ในหมวดหมู่เดียว
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal buyPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(nullable = false)
    private Integer stockQty;

    @Column(nullable = false)
    private Integer monthlySales;

    // ชื่อไฟล์รูปสินค้า
    @Column(nullable = false, length = 255)
    private String imageFilename;

    @Column(nullable = false)
    private Instant createdAt;
}
