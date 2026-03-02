package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ผู้กระทำ
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String role; // ADMIN / OWNER / STAFF

    // การกระทำ
    @Column(nullable = false, length = 30)
    private String action; // CREATE / UPDATE / DELETE / SELL / SEND_EMAIL

    // สิ่งที่กระทำ
    @Column(nullable = false, length = 30)
    private String target; // PRODUCT / CATEGORY / SALE / EMAIL

    @Column(name = "target_id")
    private Long targetId; // id ของ entity (ถ้ามี)

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
