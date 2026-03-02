package com.example.grocery.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;

    // ADMIN / OWNER / STAFF
    private String role;
}
