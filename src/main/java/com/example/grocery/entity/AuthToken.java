package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "auth_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // เจ้าของ token
    @Column(nullable = false)
    private Long userId;

    // เก็บ hash เท่านั้น (ห้ามเก็บ raw token)
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    // เวลาออก token
    @Column(nullable = false)
    private Instant issuedAt;

    // วันหมดอายุ
    @Column(nullable = false)
    private Instant expiresAt;

    // logout / revoke
    @Column
    private Instant revokedAt;
}
