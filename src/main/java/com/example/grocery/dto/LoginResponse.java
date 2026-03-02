package com.example.grocery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LoginResponse {
    private boolean success;
    private String message;

    private Long id;
    private String username;
    private String email;
    private String role;

    // ✅ เพิ่ม
    private String accessToken;
    private Instant expiresAt;
}
