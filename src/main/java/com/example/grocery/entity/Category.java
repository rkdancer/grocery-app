package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    // ชื่อไฟล์รูปที่เก็บในเครื่อง/เซิร์ฟเวอร์
    @Column(nullable = false, length = 255)
    private String imageFilename;

    @Column(nullable = false)
    private Instant createdAt;
}
