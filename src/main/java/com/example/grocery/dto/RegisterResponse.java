package com.example.grocery.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private boolean success;
    private String message;
    private Long id;
    private String username;
    private String email;

    // ADMIN / OWNER / STAFF
    private String role;
}
