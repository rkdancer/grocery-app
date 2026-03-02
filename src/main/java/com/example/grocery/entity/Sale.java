package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sold_by", nullable = false)
    private Long soldBy;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    // ✅ เปลี่ยนเป็น BigDecimal
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SaleItem> items;

    @PrePersist
    void prePersist() {
        if (soldAt == null) soldAt = Instant.now();
        if (totalPrice == null) totalPrice = BigDecimal.ZERO;
    }
}
