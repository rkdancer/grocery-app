package com.example.grocery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String otpHash;

    // ✅ เพิ่ม: send_count (ห้ามเป็น null เพราะ DB บังคับ)
    @Column(name = "send_count", nullable = false)
    private Integer sendCount;

    @Column(nullable = false)
    private Integer verifyFailCount;

    @Column(nullable = false)
    private Boolean used;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    // token สำหรับขั้น reset password
    @Column(nullable = true)
    private String resetToken;
}
